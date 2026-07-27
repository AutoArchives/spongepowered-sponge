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
package org.spongepowered.common.world.generation.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.world.generation.feature.PlacementModifier;
import org.spongepowered.api.world.generation.feature.PlacementModifierType;

import java.io.IOException;

/**
 * Wraps an entry of the game's {@code worldgen/placement_modifier_type} registry, which holds
 * bare {@link MapCodec}s and therefore cannot be mixed into directly.
 *
 * <p>The codec is resolved on demand: this type is created while the game's built-in
 * registries are still being bootstrapped, before their values are bound.</p>
 */
public record SpongePlacementModifierType(
    ResourceKey<MapCodec<? extends net.minecraft.world.level.levelgen.placement.PlacementModifier>> key
) implements PlacementModifierType {

    private MapCodec<? extends net.minecraft.world.level.levelgen.placement.PlacementModifier> codec() {
        return BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.getValueOrThrow(this.key);
    }

    @Override
    public PlacementModifier configure(final DataView config) throws IllegalArgumentException {
        try {
            final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(config));
            return (PlacementModifier) this.codec().codec().parse(SpongeFeatureType.ops(), json).getOrThrow(IllegalArgumentException::new);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read configuration: " + config, e);
        }
    }
}
