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
package org.spongepowered.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.accessor.world.DamageSourcesAccessor;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageSources;
import org.spongepowered.common.mixin.core.block.BlockMixin;

import java.util.function.Consumer;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin extends BlockMixin {

    @WrapOperation(method = "entityInside",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/InsideBlockEffectApplier;runAfter(Lnet/minecraft/world/entity/InsideBlockEffectType;Ljava/util/function/Consumer;)V"
        ))
    private void impl$spongeRedirectForFireDamage(
        final InsideBlockEffectApplier instance, final InsideBlockEffectType insideBlockEffectType,
        final Consumer<Entity> entityConsumer, final Operation<Void> original, final BlockState state,
        final Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier sameApplier
    ) {
        if (world.isClientSide()) {
            original.call(instance, insideBlockEffectType, entityConsumer);
            return;
        }
        // We'll be replacing the DamageSources.inFire() with our own temporarily then roll it back...
        final var originalInFire = world.damageSources().inFire();
        final var blockSource = SpongeDamageSources.createBlockBasedDamageSource((ServerWorld) world, pos, originalInFire);
        try {
            ((DamageSourcesAccessor) world.damageSources()).accessor$setInFire(blockSource);
            original.call(instance, insideBlockEffectType, entityConsumer);
        } finally {
            ((DamageSourcesAccessor) world.damageSources()).accessor$setInFire(originalInFire);
        }
    }

}
