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
package org.spongepowered.forge.applaunch.transformation;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.common.applaunch.transformation.SuperclassChangeTransformer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

public class ForgeSuperclassChangeTransformer extends SuperclassChangeTransformer implements ITransformer<ClassNode> {
    private static final String MIXIN_PLUGIN_REASON = "mixin";
    private static final String[] LABELS = { SuperclassChangeTransformer.NAME };

    @Override
    public String[] labels() {
        return LABELS;
    }

    @Override
    public ClassNode transform(final ClassNode input, final ITransformerVotingContext context) {
        this.transform(input);
        return input;
    }

    @Override
    public TransformerVoteResult castVote(final ITransformerVotingContext context) {
        return switch (context.getReason()) {
            case ITransformerActivity.CLASSLOADING_REASON, ForgeSuperclassChangeTransformer.MIXIN_PLUGIN_REASON -> TransformerVoteResult.YES;
            default -> TransformerVoteResult.NO;
        };
    }

    @Override
    public Set<Target> targets() {
        return this.targetClasses().stream().map(Target::targetClass).collect(Collectors.toSet());
    }

    @Override
    protected Collection<URL> collectResources() {
        final Collection<URL> resources = new ArrayList<>();
        for (ModFileInfo fileInfo : LoadingModList.get().getModFiles()) {
            final SecureJar jar = fileInfo.getFile().getSecureJar();
            final Attributes attributes = jar.moduleDataProvider().getManifest().getMainAttributes();

            final String attribute = attributes.getValue(SuperclassChangeTransformer.MANIFEST_ATTRIBUTE);
            if (attribute != null) {
                for (final String path : attribute.split(",")) {
                    try {
                        resources.add(jar.getPath(path).toUri().toURL());
                    } catch (final MalformedURLException e) {
                        LOGGER.warn("Failed to locate superclass changer {} from {}", path, fileInfo.getFile().getFileName(), e);
                    }
                }
            }
        }
        return resources;
    }
}
