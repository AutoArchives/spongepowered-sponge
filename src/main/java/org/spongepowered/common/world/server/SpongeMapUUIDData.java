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
package org.spongepowered.common.world.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores Sponge's map ID → UUID index and player UUID table as server-global SavedData.
 * Previously stored in {@code SpongeData} within {@code level.dat}.
 *
 * <p>Persisted at {@code data/sponge/map_uuids.dat} in the server-level data directory.</p>
 */
public final class SpongeMapUUIDData extends SavedData {

    // NBT CompoundTag keys must be strings, so use string-based int codec for map keys
    private static final Codec<Map<Integer, UUID>> MAP_INDEX_CODEC =
        Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, String::valueOf), UUIDUtil.CODEC);

    public static final Codec<SpongeMapUUIDData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                MAP_INDEX_CODEC.optionalFieldOf("map_uuids", Map.of()).forGetter(SpongeMapUUIDData::mapIndex),
                MAP_INDEX_CODEC.optionalFieldOf("player_uuids", Map.of()).forGetter(SpongeMapUUIDData::playerIndex)
            )
            .apply(i, SpongeMapUUIDData::new)
    );

    public static final SavedDataType<SpongeMapUUIDData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("sponge", "map_uuids"), SpongeMapUUIDData::new, CODEC, DataFixTypes.LEVEL
    );

    private final BiMap<Integer, UUID> mapUUIDIndex;
    private final BiMap<Integer, UUID> playerUUIDIndex;

    public SpongeMapUUIDData() {
        this.mapUUIDIndex = HashBiMap.create();
        this.playerUUIDIndex = HashBiMap.create();
    }

    public SpongeMapUUIDData(final Map<Integer, UUID> mapIndex, final Map<Integer, UUID> playerIndex) {
        this.mapUUIDIndex = HashBiMap.create(mapIndex);
        this.playerUUIDIndex = HashBiMap.create(playerIndex);
    }

    private Map<Integer, UUID> mapIndex() {
        return new HashMap<>(this.mapUUIDIndex);
    }

    private Map<Integer, UUID> playerIndex() {
        return new HashMap<>(this.playerUUIDIndex);
    }

    public BiMap<Integer, UUID> mapUUIDIndex() {
        return this.mapUUIDIndex;
    }

    public BiMap<Integer, UUID> playerUUIDIndex() {
        return this.playerUUIDIndex;
    }
}
