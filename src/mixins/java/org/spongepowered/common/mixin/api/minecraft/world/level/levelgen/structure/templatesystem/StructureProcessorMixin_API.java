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
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.world.generation.structure.jigsaw.Processor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;

import java.io.IOException;

@Mixin(StructureProcessor.class)
public interface StructureProcessorMixin_API extends Processor {

    // @formatter:off
    @Shadow MapCodec<? extends StructureProcessor> codec();
    // @formatter:on

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default ResourceKey type() {
        final Identifier api$location = SpongeCommon.server().registryAccess()
            .lookupOrThrow(Registries.STRUCTURE_PROCESSOR)
            .getKey((MapCodec) this.codec());
        if (api$location == null) {
            throw new IllegalStateException("Processor codec is not registered: " + this.codec());
        }
        return (ResourceKey) (Object) api$location;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default DataContainer toContainer() {
        final MapCodec api$codec = this.codec();
        final JsonElement api$serialized = (JsonElement) api$codec.codec()
            .encodeStart(SpongeCommon.server().registryAccess().createSerializationContext(JsonOps.INSTANCE), this)
            .getOrThrow();
        try {
            return DataFormats.JSON.get().read(api$serialized.toString());
        } catch (final IOException e) {
            throw new IllegalStateException("Could not read deserialized Processor:\n" + api$serialized, e);
        }
    }
}
