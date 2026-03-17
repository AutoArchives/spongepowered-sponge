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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.SpongeCommon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles migration of old Sponge world data (UUID, key) from the pre-26.1 format
 * to the new per-dimension {@link SpongeRegistryData} SavedData format.
 *
 * <p>The migration flow is:</p>
 * <ol>
 *   <li>{@code FileFixerUpperMixin} extracts UUIDs from old {@code SpongeData} in level.dat
 *       and per-dimension level.dat files BEFORE vanilla's file fixer rewrites them.</li>
 *   <li>Cached identities are stored via {@link #cacheIdentities(Map)}.</li>
 *   <li>{@code MinecraftServerMixin.loadLevel()} calls {@link #writeCachedRegistryData(Path)}
 *       AFTER the file fix completes but BEFORE any ServerLevel is created.</li>
 * </ol>
 */
public final class SpongeLevelMigration {

    // Static fields (not ThreadLocal) because fix() runs on "main" thread
    // but loadLevel() runs on "Server thread".
    private static volatile @Nullable Map<ResourceKey<Level>, UUID> cachedIdentities;
    private static volatile @Nullable CompoundTag cachedGlobalSpongeData;

    private SpongeLevelMigration() {
    }

    public static void cacheIdentities(final Map<ResourceKey<Level>, UUID> identities) {
        cachedIdentities = identities;
    }

    public static void cacheGlobalSpongeData(final CompoundTag spongeData) {
        cachedGlobalSpongeData = spongeData;
    }

    /**
     * Writes cached migrated identities to per-dimension {@code data/sponge/registry.dat} files.
     * Must be called AFTER FileFixerUpper.fix() completes (directory structure is final)
     * and BEFORE any ServerLevel is created (so SavedDataStorage finds files on disk).
     */
    public static void writeCachedRegistryData(final Path worldDir) {
        final @Nullable Map<ResourceKey<Level>, UUID> identities = cachedIdentities;
        if (identities == null || identities.isEmpty()) {
            cachedIdentities = null;
            return;
        }
        cachedIdentities = null;

        for (final var entry : identities.entrySet()) {
            final ResourceKey<Level> dimKey = entry.getKey();
            final UUID uuid = entry.getValue();
            try {
                final Path dimDir = DimensionType.getStorageFolder(dimKey, worldDir);
                final Path dataDir = dimDir.resolve("data").resolve("sponge");
                final Path registryFile = dataDir.resolve("registry.dat");
                if (Files.exists(registryFile)) {
                    continue;
                }
                Files.createDirectories(dataDir);
                final SpongeRegistryData registryData = new SpongeRegistryData(uuid, Optional.of(dimKey));
                final var encoded = SpongeRegistryData.CODEC.encodeStart(NbtOps.INSTANCE, registryData);
                if (encoded.isSuccess()) {
                    final CompoundTag savedDataTag = new CompoundTag();
                    savedDataTag.put("data", encoded.getOrThrow());
                    NbtUtils.addCurrentDataVersion(savedDataTag);
                    NbtIo.writeCompressed(savedDataTag, registryFile);
                    SpongeCommon.logger().info("[Sponge]   Wrote migrated {} UUID {} -> {}", dimKey.identifier(), uuid, registryFile);
                }
            } catch (final IOException e) {
                SpongeCommon.logger().warn("[Sponge]   Failed to write registry.dat for {}", dimKey.identifier(), e);
            }
        }

        // Write global Sponge data (MapUUIDs, player-uuid-table) to server-level SavedData
        writeGlobalSpongeData(worldDir);
    }

    private static void writeGlobalSpongeData(final Path worldDir) {
        final @Nullable CompoundTag spongeData = cachedGlobalSpongeData;
        cachedGlobalSpongeData = null;
        if (spongeData == null) {
            return;
        }

        final Path dataDir = worldDir.resolve("data").resolve("sponge");
        final Path mapUUIDFile = dataDir.resolve("map_uuids.dat");
        if (Files.exists(mapUUIDFile)) {
            return; // Already migrated
        }

        // Extract MapUUIDs and player-uuid-table from old SpongeData
        final var mapUUIDs = spongeData.getCompound("MapUUIDs").orElse(null);
        final var playerTable = spongeData.getList("player-uuid-table").orElse(null);

        // Only write if there's actual data to migrate
        final boolean hasMapData = mapUUIDs != null && !mapUUIDs.isEmpty();
        final boolean hasPlayerData = playerTable != null && !playerTable.isEmpty();
        if (!hasMapData && !hasPlayerData) {
            return;
        }

        try {
            Files.createDirectories(dataDir);
            // Write as a raw compound since the old format doesn't match the new codec exactly.
            // The SpongeMapUUIDData.CODEC will read the proper format; here we just preserve the raw data
            // for potential manual migration. The new SavedData will be created fresh on first access.
            final CompoundTag savedDataTag = new CompoundTag();
            final CompoundTag data = new CompoundTag();
            if (hasMapData) {
                data.put("legacy_map_uuids", mapUUIDs);
            }
            if (hasPlayerData) {
                data.put("legacy_player_uuids", playerTable);
            }
            savedDataTag.put("data", data);
            NbtUtils.addCurrentDataVersion(savedDataTag);
            NbtIo.writeCompressed(savedDataTag, mapUUIDFile);
            SpongeCommon.logger().info("[Sponge]   Preserved global MapUUIDs/player-uuid-table -> {}", mapUUIDFile);
        } catch (final IOException e) {
            SpongeCommon.logger().warn("[Sponge]   Failed to write global Sponge data", e);
        }
    }
}
