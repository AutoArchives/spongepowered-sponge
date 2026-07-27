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
package org.spongepowered.common.world.generation.carver;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.world.generation.carver.Carver;
import org.spongepowered.api.world.generation.carver.CarverType;
import org.spongepowered.common.SpongeCommon;

import java.io.IOException;

/**
 * Wraps an entry of the game's {@code worldgen/carver_type} registry, which holds bare
 * {@link MapCodec}s and therefore cannot be mixed into directly.
 *
 * <p>The codec is resolved on demand: this type is created while the game's built-in
 * registries are still being bootstrapped, before their values are bound.</p>
 */
public record SpongeCarverType(ResourceKey<MapCodec<? extends WorldCarver>> key) implements CarverType {

    private MapCodec<? extends WorldCarver> codec() {
        return BuiltInRegistries.CARVER_TYPE.getValueOrThrow(this.key);
    }

    /**
     * Carvers are read and written while registries are still being populated, so the
     * registries backing the configuration codecs come from whichever holder is in scope
     * rather than from a running server.
     */
    public static RegistryOps<JsonElement> ops() {
        return RegistryOps.create(JsonOps.INSTANCE, SpongeCommon.scopedHolder().registryHolder());
    }

    @Override
    public Carver configure(final DataView config) {
        try {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(config));
            return (Carver) this.codec().codec().parse(SpongeCarverType.ops(), json).getOrThrow(IllegalArgumentException::new);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read configuration: " + config, e);
        }
    }
}
