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
package org.spongepowered.common.mixin.core.world.ticks;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.TickPriority;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.data.DataUtil;
import org.spongepowered.common.data.holder.SpongeMutableDataHolder;

import java.util.Optional;

@Mixin(SavedTick.class)
public abstract class SavedTickMixin<T> implements SpongeMutableDataHolder, DataCompoundHolder, CreatorTrackedBridge {

    private @Nullable CompoundTag impl$compound;

    @Override
    public CompoundTag data$getCompound() {
        return this.impl$compound;
    }

    @Override
    public void data$setCompound(final CompoundTag nbt) {
        this.impl$compound = nbt;
    }

    /**
     * @return The codec
     * @author gabizou - 25w07a - February 15th, 2025
     * @reason Because SavedTicks are now saved via Codec, we have to modify the codec to inject our
     * custom data alongside it. This means that we're explicitly not going to be calling the original
     * operation, but merely adding the existing products and instantiation function. It is why we use
     * a {@link Redirect} instead of a {@link WrapOperation}. Unless there is a better way to modify
     * the codec to support the data field additions.
     */
    @SuppressWarnings("deprecation")
    @Redirect(method = "lambda$codec$1", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/Products$P4;apply(Lcom/mojang/datafixers/kinds/Applicative;Lcom/mojang/datafixers/util/Function4;)Lcom/mojang/datafixers/kinds/App;"))
    private static <T> App<RecordCodecBuilder. Mu<SavedTick<T>>, SavedTick<T>> lambda$codec$1(
        final Products.P4<RecordCodecBuilder.Mu<SavedTick<T>>, T, BlockPos, Integer, TickPriority> products,
        final Applicative<RecordCodecBuilder.Mu<SavedTick<T>>, ?> instance,
        final Function4<T, BlockPos, Integer, TickPriority, SavedTick<T>> function
    ) {
        final RecordCodecBuilder<SavedTick<T>, Optional<CustomData>> spongeCustomData = Codec.optionalField("customData", CustomData.CODEC, true).forGetter(t -> {
            final var bridge = ((SavedTickMixin<T>) (Object) t);
            DataUtil.syncDataToTag(bridge);
            if (bridge.impl$compound != null) {
                return Optional.of(CustomData.of(bridge.impl$compound));
            }
            return Optional.empty();
        });
        return products.and(spongeCustomData)
                .apply(instance, (t, blockPos, integer, tickPriority, customData) -> {
                    final var tick = function.apply(t, blockPos, integer, tickPriority);
                    customData.ifPresent(data -> {
                        ((DataCompoundHolder) (Object) tick).data$setCompound(data.getUnsafe());
                        DataUtil.syncTagToData(tick);
                        ((DataCompoundHolder) (Object) tick).data$setCompound(null);
                    });
                    return tick;
                });
    }
}
