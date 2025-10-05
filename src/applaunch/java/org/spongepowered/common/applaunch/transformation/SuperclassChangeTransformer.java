/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.applaunch.transformation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SuperclassChangeTransformer {
    public static final String NAME = "superclass_change";
    public static final String EXTENSION = "superclasschange";
    public static final String MANIFEST_ATTRIBUTE = "Superclass-Transformer";

    protected static final Logger LOGGER = LogManager.getLogger();

    private @MonotonicNonNull Map<String, String> superclassMap;

    private static Map<String, String> readMap(final URL resource) throws IOException {
        final Map<String, String> targets = new HashMap<>();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(":", 2);
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid map entry: " + line);
                    }
                    targets.put(parts[0], parts[1]);
                }
            }
        }
        return targets;
    }

    protected abstract Collection<URL> collectResources();

    public final synchronized void loadResources() {
        if (this.superclassMap == null) {
            this.superclassMap = new ConcurrentHashMap<>();

            for (final URL resource : this.collectResources()) {
                if (resource.getFile().endsWith(SuperclassChangeTransformer.EXTENSION)) {
                    try {
                        SuperclassChangeTransformer.LOGGER.debug("Reading superclass change {}", resource);
                        this.superclassMap.putAll(SuperclassChangeTransformer.readMap(resource));
                    } catch (final Exception ex) {
                        SuperclassChangeTransformer.LOGGER.error("Failed to load superclass change {}", resource, ex);
                    }
                } else {
                    SuperclassChangeTransformer.LOGGER.warn("Offered superclass change {} that does not end with expected extension '{}'", resource, SuperclassChangeTransformer.EXTENSION);
                }
            }
        }
    }

    protected final boolean transform(final ClassNode classNode) {
        if (this.superclassMap == null) {
            throw new IllegalStateException("Resources not loaded");
        }

        final String inputKey = classNode.name.replace("/", ".");
        final String newSuperclass = this.superclassMap.get(inputKey);
        if (newSuperclass == null) {
            SuperclassChangeTransformer.LOGGER.warn("No superclass change for {}", inputKey);
            return false;
        }

        final String sanitizedSuperClass = newSuperclass.replace('.', '/');
        classNode.methods.forEach(m -> SuperclassChangeTransformer.transformMethod(m, classNode.superName, sanitizedSuperClass));
        classNode.superName = sanitizedSuperClass;
        return true;
    }

    private static void transformMethod(final MethodNode node, final String originalSuperclass, final String superClass) {
        for (final MethodInsnNode insn : SuperclassChangeTransformer.findSuper(node, originalSuperclass)) {
            insn.owner = superClass;
        }
    }

    private static List<MethodInsnNode> findSuper(final MethodNode method, final String originalSuperClass) {
        final List<MethodInsnNode> nodes = new ArrayList<>();
        for (final AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() == Opcodes.INVOKESPECIAL && originalSuperClass.equals(
                ((MethodInsnNode) node).owner)) {
                nodes.add((MethodInsnNode) node);
            }
        }
        return nodes;
    }

    protected final Set<String> targetClasses() {
        this.loadResources();
        return this.superclassMap.keySet();
    }
}
