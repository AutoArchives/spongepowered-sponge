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

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;

public abstract class AccessWidenerTransformer {
    public static final String NAME = "access_widener";
    public static final String EXTENSION = "accesswidener";
    public static final String MANIFEST_ATTRIBUTE = "Access-Widener";

    protected static final Logger LOGGER = LogManager.getLogger();

    private @MonotonicNonNull AccessWidener widener;

    protected final ClassNode transform(final ClassNode input) {
        if (this.widener == null) {
            throw new IllegalStateException("Resources not loaded");
        }
        AccessWidenerTransformer.LOGGER.debug("Transforming {}", input.name);
        final ClassNode output = new ClassNode(Opcodes.ASM9);
        final ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, output, this.widener);
        input.accept(visitor);
        return output;
    }

    protected abstract Collection<URL> collectResources();

    public final synchronized void loadResources() {
        if (this.widener == null) {
            this.widener = new AccessWidener();
            final AccessWidenerReader awReader = new AccessWidenerReader(this.widener);

            for (final URL resource : this.collectResources()) {
                if (resource.getFile().endsWith(AccessWidenerTransformer.EXTENSION)) {
                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                        AccessWidenerTransformer.LOGGER.debug("Reading access widener {}", resource);
                        awReader.read(reader);
                    } catch (final IOException ex) {
                        AccessWidenerTransformer.LOGGER.error("Failed to load access widener {}", resource, ex);
                    }
                } else {
                    AccessWidenerTransformer.LOGGER.warn("Offered access widener {} that does not end with expected extension '{}'", resource, AccessWidenerTransformer.EXTENSION);
                }
            }
        }
    }

    protected final Set<String> targetClasses() {
        this.loadResources();
        return this.widener.getTargets();
    }
}
