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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.common.accessor.server.bossevents.CustomBossEventsAccessor;
import org.spongepowered.common.config.inheritable.InheritableConfigHandle;
import org.spongepowered.common.config.inheritable.WorldConfig;

import java.util.Optional;
import java.util.UUID;

public final class SpongeServerLevelData {

    public static final Codec<SpongeServerLevelData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                WorldOptions.CODEC.fieldOf("world_gen_options").stable().forGetter(SpongeServerLevelData::worldGenOptions),
                CustomBossEventsAccessor.accessor$codec().optionalFieldOf("custom_boss_events").stable().forGetter(SpongeServerLevelData::customBossEvents),
                UUIDUtil.CODEC.fieldOf("uuid").stable().forGetter(SpongeServerLevelData::uniqueId),
                net.minecraft.resources.ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("key").stable().forGetter(SpongeServerLevelData::resourceKey)
            )
            .apply(i, i.stable(SpongeServerLevelData::new))
    );
    private @Nullable ResourceKey key;
    private UUID uniqueId = UUID.randomUUID();
    private @Nullable InheritableConfigHandle<WorldConfig> configAdapter;
    private WorldOptions worldGenOptions;
    private @Nullable CustomBossEvents customBossEvents;

    public SpongeServerLevelData() {
    }

    public SpongeServerLevelData(WorldOptions worldOptions, Optional<CustomBossEvents> customBossEvents, UUID uuid, Optional<net.minecraft.resources.ResourceKey<Level>> levelResourceKey) {
        this.worldGenOptions = worldOptions;
        this.customBossEvents = customBossEvents.orElse(null);
        this.uniqueId = uuid;
        this.key = (ResourceKey) levelResourceKey.orElse(null);
    }

    public @Nullable ResourceKey key() {
        return this.key;
    }

    public Optional<net.minecraft.resources.ResourceKey<Level>> resourceKey() {
        return Optional.ofNullable((net.minecraft.resources.ResourceKey<Level>) (Object) this.key);
    }

    public void setKey(final ResourceKey key) {
        this.key = key;
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(final UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setWorldGenOptions(final WorldGenSettings worldGenSettings) {
        this.worldGenOptions = worldGenSettings.options();
    }

    public WorldOptions worldGenOptions() {
        return this.worldGenOptions;
    }

    public @Nullable InheritableConfigHandle<WorldConfig> configAdapter() {
        return this.configAdapter;
    }

    public void setConfigAdapter(final InheritableConfigHandle<WorldConfig> adapter) {
        this.configAdapter = adapter;
    }

    public Optional<CustomBossEvents> customBossEvents() {
        return Optional.ofNullable(this.customBossEvents);
    }

    public void setCustomBossEvents(final CustomBossEvents customBossEvents) {
        this.customBossEvents = customBossEvents;
    }
}
