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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A {@link SavedData} implementation that stores per-dimension Sponge identity,
 * consisting of a unique {@link UUID} and a {@link ResourceKey} for the world.
 *
 * <p>This is persisted as {@code data/sponge/registry.dat} within each dimension's
 * saved data directory.</p>
 */
public final class SpongeRegistryData extends SavedData {

    public static final Codec<SpongeRegistryData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(SpongeRegistryData::uniqueId),
                ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("key").forGetter(SpongeRegistryData::resourceKey)
            )
            .apply(i, SpongeRegistryData::new)
    );

    public static final SavedDataType<SpongeRegistryData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("sponge", "registry"), SpongeRegistryData::new, CODEC, DataFixTypes.LEVEL
    );

    private UUID uniqueId;
    private @Nullable ResourceKey<Level> key;

    public SpongeRegistryData() {
        this.uniqueId = UUID.randomUUID();
    }

    public SpongeRegistryData(final UUID uniqueId, final Optional<ResourceKey<Level>> key) {
        this.uniqueId = uniqueId;
        this.key = key.orElse(null);
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(final UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.setDirty();
    }

    public org.spongepowered.api.@Nullable ResourceKey key() {
        if (this.key == null) {
            return null;
        }
        return (org.spongepowered.api.ResourceKey) (Object) this.key.identifier();
    }

    public Optional<ResourceKey<Level>> resourceKey() {
        return Optional.ofNullable(this.key);
    }

    public void setKey(final org.spongepowered.api.ResourceKey key) {
        this.key = SpongeWorldManager.createRegistryKey(key);
        this.setDirty();
    }
}
