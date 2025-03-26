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
package org.spongepowered.forge.mixin.core.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.util.DependencySorter;
import org.spongepowered.api.registry.Registry;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.accessor.resources.RegistryDataLoader_LoaderAccessor;
import org.spongepowered.common.bridge.core.WritableRegistryBridge;
import org.spongepowered.common.bridge.resources.RegistryDataLoader_LoaderBridge;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.launch.Lifecycle;
import org.spongepowered.common.registry.SpongeRegistryDependencyEntry;
import org.spongepowered.common.registry.SpongeRegistryHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin_Forge {


    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 1))
    private static void impl$onLoad(final List<RegistryDataLoader_LoaderAccessor<?>> instance, final Consumer<?> consumer, final Operation<Void> original) {
        // TODO: Remote layer
        final DependencySorter<RegistryType<?>, SpongeRegistryDependencyEntry<RegistryDataLoader_LoaderAccessor<?>>> dependencies = new DependencySorter<>();
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        instance.stream()
            .filter(l -> ((RegistryDataLoader_LoaderBridge) l).bridge$registryHolder() != null)
            .collect(Collectors.groupingBy(l -> ((RegistryDataLoader_LoaderBridge) l).bridge$registryHolder(), Collectors.toSet()))
            .forEach((k, v) -> {
                ((SpongeRegistryHolder) k).setRootMinecraftRegistry(new RegistryAccess.ImmutableRegistryAccess(
                    (List) Stream.concat(k.streamRegistries(RegistryRoots.MINECRAFT), v.stream().map(RegistryDataLoader_LoaderAccessor::accessor$registry)).toList()));
                lifecycle.processServerRegistries(k, v.stream()
                    .filter(l -> !RegistryDataLoader.DIMENSION_REGISTRIES.contains(l.accessor$data())) // NOTE: DIMENSION_REGISTRIES are special!
                    .map(l -> (Registry<?>) l.accessor$registry()));
                v.forEach(l -> dependencies.addEntry(((Registry<?>) l.accessor$registry()).type(),
                    new SpongeRegistryDependencyEntry<>(l, ((WritableRegistryBridge<?>) l.accessor$registry()).bridge$pendingDependencies().toList())));
            });

        List<RegistryDataLoader_LoaderAccessor<?>> loaders = new ArrayList<>(instance.size());
        dependencies.orderByDependencies(($, v) -> loaders.add(v.cookie()));

        original.call(Collections.unmodifiableList(loaders), consumer);
    }
}
