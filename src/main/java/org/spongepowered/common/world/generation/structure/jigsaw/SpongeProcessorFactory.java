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
package org.spongepowered.common.world.generation.structure.jigsaw;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.world.generation.structure.jigsaw.Processor;
import org.spongepowered.common.registry.SpongeRegistryHolder;

import java.io.IOException;
import java.util.Objects;

public final class SpongeProcessorFactory implements Processor.Factory {

    @Override
    public Processor parse(final RegistryHolder registries, final ResourceKey id, final DataView config) throws IOException {
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(config, "config");

        final SpongeRegistryHolder spongeHolder = (SpongeRegistryHolder) registries;
        final HolderLookup.RegistryLookup<MapCodec<? extends StructureProcessor>> lookup =
            spongeHolder.lookupOrThrow(Registries.STRUCTURE_PROCESSOR);

        final Identifier identifier = Identifier.fromNamespaceAndPath(id.namespace(), id.value());
        final net.minecraft.resources.ResourceKey<MapCodec<? extends StructureProcessor>> vanillaKey =
            net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_PROCESSOR, identifier);
        final MapCodec<? extends StructureProcessor> codec = lookup.get(vanillaKey)
            .orElseThrow(() -> new IOException("Unknown structure processor id: " + id))
            .value();

        final RegistryOps<JsonElement> ops = spongeHolder.createSerializationContext(JsonOps.INSTANCE);
        final JsonElement json = JsonParser.parseString(DataFormats.JSON.get().write(config));
        final StructureProcessor processor = codec.codec().parse(ops, json).getOrThrow(
            message -> new IllegalArgumentException("Failed to parse processor " + id + ": " + message));
        return (Processor) processor;
    }
}
