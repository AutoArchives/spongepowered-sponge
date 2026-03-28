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
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import org.spongepowered.api.registry.Registry;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.resources.RegistryDataLoader_ResourceManagerRegistryLoadTaskAccessor;
import org.spongepowered.common.accessor.resources.RegistryLoadTaskAccessor;
import org.spongepowered.common.bridge.core.WritableRegistryBridge;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.launch.Lifecycle;
import org.spongepowered.common.registry.SpongeRegistryDependencyEntry;
import org.spongepowered.common.registry.SpongeRegistryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin_Forge {

    // Forge's lambda$load$2 is the freeze-step lambda in the async load pipeline.
    // Unlike NeoForge, Forge doesn't have a gametest hook before the freeze logic,
    // so we can target stream() ordinal 0 directly with no @Slice.
    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "lambda$load$2",
        at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;", ordinal = 0))
    private static <T> Stream<T> impl$onLoad(
        List<RegistryLoadTask<T>> instance, Operation<Stream> original
    ) {
        // TODO: Remote layer
        final DependencySorter<RegistryType<?>, SpongeRegistryDependencyEntry<RegistryLoadTaskAccessor<?>>> dependencies = new DependencySorter<>();
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        final var collect = instance.stream()
            .collect(Collectors.groupingBy(l -> {
                if (l instanceof RegistryDataLoader_ResourceManagerRegistryLoadTaskAccessor accessor) {
                    return accessor.accessor$getResourceManager();
                }
                return ResourceManager.Empty.INSTANCE;
            }, Collectors.toSet()));
        collect
            .forEach((k, v) -> {
                final RegistryHolder registryHolder;
                if (k == ResourceManager.Empty.INSTANCE) {
                    // on the client side, we don't have a real resource manager
                    registryHolder = SpongeCommon.game();
                } else {
                    registryHolder = (RegistryHolder) k;
                }
                // Build a combined registry list: new batch registries replace any existing ones
                // with the same key (e.g., on re-join, the second load produces fresh registries
                // for the same keys that were already in the root from the first load).
                final var batchRegistries = v.stream()
                    .map(RegistryLoadTaskAccessor.class::cast)
                    .map(RegistryLoadTaskAccessor::accessor$registry)
                    .toList();
                final var batchKeys = batchRegistries.stream()
                    .map(net.minecraft.core.Registry::key)
                    .collect(java.util.stream.Collectors.toSet());
                ((SpongeRegistryHolder) registryHolder).setRootMinecraftRegistry(new RegistryAccess.ImmutableRegistryAccess(
                    (List) Stream.concat(
                        registryHolder.streamRegistries(RegistryRoots.MINECRAFT)
                            .filter(r -> !batchKeys.contains(((net.minecraft.core.Registry<?>) r).key())),
                        batchRegistries.stream()
                    ).toList()));

                // Only fire plugin registry events on the server side (ResourceManager-backed loads).
                // Client-side network loads only receive SYNCHRONIZED_REGISTRIES and lack server-only
                // registries (placed_feature, configured_feature, etc.) that plugins may declare as
                // dependencies, causing freeze validation to fail.
                if (k != ResourceManager.Empty.INSTANCE) {
                    lifecycle.processServerRegistries(registryHolder, v.stream()
                            .map(RegistryLoadTaskAccessor.class::cast)
                        .filter(l -> !RegistryDataLoader.DIMENSION_REGISTRIES.contains(l.accessor$data())) // NOTE: DIMENSION_REGISTRIES are special!
                        .map(l -> (Registry<?>) l.accessor$registry()));
                }

                v
                    .stream().map(RegistryLoadTaskAccessor.class::cast)
                    .forEach(l -> dependencies.addEntry(((Registry<?>) l.accessor$registry()).type(),
                    new SpongeRegistryDependencyEntry<>(l, ((WritableRegistryBridge<?>) l.accessor$registry()).bridge$pendingDependencies().toList())));
            });

        List<RegistryLoadTaskAccessor<?>> loaders = new ArrayList<>(collect.size());
        dependencies.orderByDependencies(($, v) -> loaders.add(v.cookie()));

        return original.call(loaders);
    }
}
