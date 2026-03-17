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
package org.spongepowered.common.mixin.core.util.filefix;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.filefix.FileFixerUpper;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.world.server.SpongeLevelMigration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mixin on {@link FileFixerUpper} to preserve Sponge's per-world identity (UUID, key)
 * during vanilla's file-fixing process, which rewrites {@code level.dat} and strips
 * unknown tags including the {@code SpongeData} compound.
 *
 * <p>This also cleans up old Sponge-created files ({@code DIM-1/level.dat},
 * {@code DIM1/level.dat}, {@code session.lock}) so that vanilla's
 * {@code DimensionStorageFileFix} can fully delete the legacy directories.</p>
 */
@Mixin(FileFixerUpper.class)
public abstract class FileFixerUpperMixin {

    @Unique private static final Logger SPONGE_LOGGER = LogUtils.getLogger();

    // TODO: Also cache MapUUIDs and player-uuid-table from old SpongeData for migration
    //       to separate SavedData files (sponge:map_uuids, etc.) — see follow-up task

    /**
     * Extracts SpongeData from level.dat and per-dimension level.dat files BEFORE
     * vanilla's file fixer rewrites them. Also cleans up old Sponge files so
     * vanilla can fully delete DIM-1/DIM1 directories.
     */
    @Inject(method = "fix(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lcom/mojang/serialization/Dynamic;Lnet/minecraft/util/worldupdate/UpgradeProgress;)Lcom/mojang/serialization/Dynamic;",
        at = @At("HEAD"))
    private void sponge$extractSpongeDataBeforeFix(
        final LevelStorageSource.LevelStorageAccess worldAccess,
        final Dynamic<?> levelDataTag,
        final UpgradeProgress upgradeProgress,
        final CallbackInfoReturnable<Dynamic<?>> cir
    ) {
        final Map<ResourceKey<Level>, UUID> identities = new HashMap<>();
        final Path worldDir = worldAccess.getLevelDirectory().path();

        // 1. Extract overworld UUID from root level.dat's SpongeData (in the Dynamic)
        //    Note: levelDataTag is the "Data" compound, but SpongeData is a SIBLING of Data
        //    at the root level. We need to re-read the raw file to get it.
        try {
            final CompoundTag root = NbtIo.readCompressed(worldDir.resolve("level.dat"), NbtAccounter.defaultQuota());
            final CompoundTag spongeData = root.getCompound("SpongeData").orElse(null);
            if (spongeData != null && !spongeData.isEmpty()) {
                sponge$extractUuid(spongeData, Level.OVERWORLD, identities);
                // Also cache MapUUIDs and player-uuid-table for global migration
                SpongeLevelMigration.cacheGlobalSpongeData(spongeData);
            }
        } catch (final IOException e) {
            // No level.dat or can't read — skip
        }

        // 2. Extract nether/end UUIDs from their per-dimension level.dat files
        //    and clean up old Sponge files so vanilla can delete the directories
        sponge$extractAndCleanDimension(worldDir, "DIM-1", Level.NETHER, identities);
        sponge$extractAndCleanDimension(worldDir, "DIM1", Level.END, identities);

        if (!identities.isEmpty()) {
            SPONGE_LOGGER.info("[Sponge] Preserved {} world UUID(s) from old SpongeData during file fix", identities.size());
            // Cache for writing AFTER the fix completes and the COW swap is done.
            // The actual write happens in MinecraftServerMixin.loadLevel() via SpongeLevelMigration.
            SpongeLevelMigration.cacheIdentities(identities);
        }
    }

    // Registry.dat writing is deferred to MinecraftServerMixin.loadLevel() HEAD inject,
    // which runs AFTER fix() completes (COW swap done) but BEFORE any ServerLevel is created.

    /**
     * Extracts a UUID from a SpongeData compound's "UUID" IntArray field.
     */
    @Unique
    private static void sponge$extractUuid(
        final CompoundTag spongeData,
        final ResourceKey<Level> dimKey,
        final Map<ResourceKey<Level>, UUID> identities
    ) {
        final int[] uuidArray = spongeData.getIntArray("UUID").orElse(null);
        if (uuidArray != null && uuidArray.length == 4) {
            final UUID uuid = new UUID(
                (long) uuidArray[0] << 32 | uuidArray[1] & 0xFFFFFFFFL,
                (long) uuidArray[2] << 32 | uuidArray[3] & 0xFFFFFFFFL
            );
            identities.put(dimKey, uuid);
        }
    }

    /**
     * Reads SpongeData from an old per-dimension level.dat (e.g. DIM-1/level.dat),
     * extracts the UUID, then deletes all Sponge-created files so vanilla's
     * DimensionStorageFileFix can fully remove the legacy directory.
     */
    @Unique
    private static void sponge$extractAndCleanDimension(
        final Path worldDir,
        final String legacyDirName,
        final ResourceKey<Level> dimKey,
        final Map<ResourceKey<Level>, UUID> identities
    ) {
        final Path legacyDir = worldDir.resolve(legacyDirName);
        final Path legacyLevelDat = legacyDir.resolve("level.dat");

        if (!Files.exists(legacyLevelDat)) {
            return;
        }

        // Extract SpongeData
        try {
            final CompoundTag root = NbtIo.readCompressed(legacyLevelDat, NbtAccounter.defaultQuota());
            final CompoundTag spongeData = root.getCompound("SpongeData").orElse(null);
            if (spongeData != null && !spongeData.isEmpty()) {
                sponge$extractUuid(spongeData, dimKey, identities);
            }
        } catch (final IOException e) {
            SPONGE_LOGGER.warn("[Sponge]   Failed to read SpongeData from {}", legacyLevelDat, e);
        }

        // Migrate any remaining SavedData files that vanilla didn't move
        // (e.g. random_sequences.dat that was left behind in DIM-1/data/)
        // Old format: DIM-1/data/random_sequences.dat (flat, no namespace)
        // New format: dimensions/minecraft/the_nether/data/minecraft/random_sequences.dat (namespaced)
        final Path legacyDataDir = legacyDir.resolve("data");
        final Path newDimDir = DimensionType.getStorageFolder(dimKey, worldDir);
        final Path newDataDir = newDimDir.resolve("data").resolve("minecraft"); // namespace subdirectory
        try {
            if (Files.exists(legacyDataDir) && Files.isDirectory(legacyDataDir)) {
                Files.createDirectories(newDataDir);
                try (var stream = Files.list(legacyDataDir)) {
                    stream.filter(Files::isRegularFile).forEach(source -> {
                        final Path target = newDataDir.resolve(source.getFileName());
                        try {
                            if (!Files.exists(target)) {
                                Files.move(source, target);
                                SPONGE_LOGGER.info("[Sponge]   Migrated {} -> {}", source.getFileName(), target);
                            }
                        } catch (final IOException e) {
                            SPONGE_LOGGER.warn("[Sponge]   Failed to migrate {}", source, e);
                        }
                    });
                }
            }
        } catch (final IOException e) {
            SPONGE_LOGGER.warn("[Sponge]   Failed to migrate data files from {}", legacyDirName, e);
        }

        // Clean up old Sponge files so vanilla can delete the legacy directory
        try {
            Files.deleteIfExists(legacyLevelDat);
            Files.deleteIfExists(legacyDir.resolve("session.lock"));
            if (Files.exists(legacyDataDir)) {
                PathUtils.deleteDirectory(legacyDataDir);
            }
            // Delete the now-empty legacy directory
            if (Files.exists(legacyDir) && Files.list(legacyDir).findFirst().isEmpty()) {
                Files.delete(legacyDir);
            }
            SPONGE_LOGGER.info("[Sponge]   Cleaned up legacy directory {}", legacyDirName);
        } catch (final IOException e) {
            SPONGE_LOGGER.warn("[Sponge]   Failed to clean up {}", legacyDirName, e);
        }
    }
}
