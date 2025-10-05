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
package org.spongepowered.neoforge.applaunch.transformation;

import net.neoforged.fml.classloading.SecureJar;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.spongepowered.common.applaunch.transformation.SuperclassChangeTransformer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.Attributes;

public class NeoSuperclassChangeTransformer extends SuperclassChangeTransformer implements ClassProcessor {
    private static final ProcessorName NAME = new ProcessorName("neoforge", SuperclassChangeTransformer.NAME);

    @Override
    public ProcessorName name() {
        return NeoSuperclassChangeTransformer.NAME;
    }

    @Override
    public boolean handlesClass(final SelectionContext context) {
        return this.targetClasses().contains(context.type().getClassName());
    }

    @Override
    public ComputeFlags processClass(final TransformationContext context) {
        this.transform(context.node());
        return ComputeFlags.SIMPLE_REWRITE;
    }

    @Override
    protected Collection<URL> collectResources() {
        final Collection<URL> resources = new ArrayList<>();
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            final SecureJar jar = fileInfo.getFile().getSecureJar();
            final Attributes attributes = jar.moduleDataProvider().getManifest().getMainAttributes();

            final String attribute = attributes.getValue(SuperclassChangeTransformer.MANIFEST_ATTRIBUTE);
            if (attribute != null) {
                for (final String path : attribute.split(",")) {
                    try {
                        resources.add(jar.contents().findFile(path).get().toURL());
                    } catch (final Exception e) {
                        LOGGER.warn("Failed to locate superclass changer {} from {}", path, fileInfo.getFile().getFileName(), e);
                    }
                }
            }
        }
        return resources;
    }
}
