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

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.objectweb.asm.Type;
import org.spongepowered.common.applaunch.transformation.ListenerTransformer;

import java.util.HashSet;
import java.util.Set;

public class NeoListenerTransformer extends ListenerTransformer implements ClassProcessor {
    private static final ProcessorName NAME = new ProcessorName("neoforge", ListenerTransformer.NAME);

    private @MonotonicNonNull Set<Type> targets;

    private synchronized void loadTargets() {
        if (this.targets == null) {
            this.targets = new HashSet<>();

            final Type listenerType = Type.getType(ListenerTransformer.ANNOTATION_DESC);
            for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
                for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                    if (listenerType.equals(annotation.annotationType())) {
                        this.targets.add(annotation.clazz());
                    }
                }
            }
        }
    }

    @Override
    public ProcessorName name() {
        return NeoListenerTransformer.NAME;
    }

    @Override
    public boolean handlesClass(final SelectionContext context) {
        this.loadTargets();
        return this.targets.contains(context.type());
    }

    @Override
    public ComputeFlags processClass(final TransformationContext context) {
        this.transform(context.node());
        return ComputeFlags.COMPUTE_FRAMES;
    }
}
