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
package org.spongepowered.common.world.storage;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.ValueInput;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.world.server.SpongeWorldManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class SpongePlayerDataManager {

    private static final String SPONGE_DATA = "sponge";
    private final Server server;
    private final Path playersDirectory;

    public SpongePlayerDataManager(final Server server) {
        this.server = server;
        this.playersDirectory = ((SpongeWorldManager) this.server.worldManager()).getDefaultWorldDirectory().resolve("data").resolve(SpongePlayerDataManager.SPONGE_DATA);
    }

    public void readLegacyPlayerData(final ServerPlayer playerEntity, final ValueInput compound, @Nullable Instant creation) {
        if (creation == null) {
            creation = Instant.now();
        }
        Instant lastPlayed = creation;
        // first try to migrate bukkit join data stuff
        final var bukkit = compound.child(Constants.Bukkit.BUKKIT);
        if (bukkit.isPresent()) {
            final var bukkitCompound = bukkit.get();
            creation = Instant.ofEpochMilli(bukkitCompound.getLongOr(Constants.Bukkit.BUKKIT_FIRST_PLAYED, 0));
            lastPlayed = Instant.ofEpochMilli(bukkitCompound.getLongOr(Constants.Bukkit.BUKKIT_LAST_PLAYED, 0));
        }
        // migrate canary join data
        final var canary = compound.child(Constants.Canary.ROOT);
        if (canary.isPresent()) {
            final var canaryCompound = canary.get();
            creation = Instant.ofEpochMilli(canaryCompound.getLongOr(Constants.Canary.FIRST_JOINED, 0));
            lastPlayed = Instant.ofEpochMilli(canaryCompound.getLongOr(Constants.Canary.LAST_JOINED, 0));
        }
        final Path playerFile = this.playersDirectory.resolve(playerEntity.uniqueId() + ".dat");
        if (Files.isReadable(playerFile)) {
            final CompoundTag playerFileCompound;
            try (final InputStream stream = Files.newInputStream(playerFile)) {
                playerFileCompound = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
                creation = playerFileCompound
                    .read(Constants.Sponge.PlayerData.PLAYER_DATA_JOIN.toString(), Constants.Sponge.PlayerData.INSTANT_CODEC)
                    .orElse(Instant.now());
                lastPlayed = playerFileCompound
                    .read(Constants.Sponge.PlayerData.PLAYER_DATA_LAST.toString(), Constants.Sponge.PlayerData.INSTANT_CODEC)
                    .orElse(Instant.now());
            } catch (final Exception e) {
                throw new RuntimeException("Failed to decompress playerdata for playerfile " + playerFile, e);
            }
        }
        playerEntity.offer(Keys.FIRST_DATE_JOINED, creation);
        playerEntity.offer(Keys.LAST_DATE_JOINED, lastPlayed);
        playerEntity.offer(Keys.LAST_DATE_PLAYED, lastPlayed);
    }

    public void deleteLegacyPlayerData(final ServerPlayer playerEntity) {
        final Path playerFile = this.playersDirectory.resolve(playerEntity.uniqueId() + ".dat");
        if (Files.isRegularFile(playerFile)) {
            try {
                Files.delete(playerFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
