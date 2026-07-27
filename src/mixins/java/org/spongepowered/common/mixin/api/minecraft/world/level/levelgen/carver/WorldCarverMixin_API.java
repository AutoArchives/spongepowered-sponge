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
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen.carver;

import com.google.gson.JsonElement;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.generation.carver.Carver;
import org.spongepowered.api.world.generation.carver.CarverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.DataPackUtil;
import org.spongepowered.common.world.generation.carver.SpongeCarverType;

import java.io.IOException;
import java.util.Optional;

@Mixin(WorldCarver.class)
public interface WorldCarverMixin_API extends Carver {

    // @formatter:off
    @Shadow MapCodec<? extends WorldCarver> shadow$codec();
    // @formatter:on

    @Override
    default CarverType type() {
        final Identifier location = BuiltInRegistries.CARVER_TYPE.getKey(this.shadow$codec());
        return Sponge.game().registry(RegistryTypes.CARVER_TYPE).value((ResourceKey) (Object) location);
    }

    @Override
    default DataView toContainer() {
        final JsonElement serialized = WorldCarver.DIRECT_CODEC
            .encodeStart(SpongeCarverType.ops(), (WorldCarver) this).getOrThrow();
        try {
            return DataFormats.JSON.get().read(serialized.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read deserialized Carver: " + serialized, e);
        }
    }

    @Override
    default Optional<DataContainer> toDataPack(final RegistryHolder registryHolder) {
        return DataPackUtil.toDataContainer(registryHolder, WorldCarver.DIRECT_CODEC, (WorldCarver) this);
    }
}
