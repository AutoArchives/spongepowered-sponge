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
package org.spongepowered.neoforge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.api.event.cause.entity.damage.DamageStepTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.common.bridge.world.entity.TrackedDamageBridge;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageTracker;

import java.util.Stack;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Neo_Damage implements TrackedDamageBridge {

    @Shadow protected Stack<DamageContainer> damageContainers;

    @Shadow public abstract ItemStack shadow$getUseItem();

    @Override
    public float damage$getContainerDamage(final float damage) {
        return this.damageContainers.peek().getNewDamage();
    }

    @Override
    public void damage$setContainerDamage(final float damage) {
        this.damageContainers.peek().setNewDamage(damage);
    }

    @ModifyVariable(method = "actuallyHurt", ordinal = 1,
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"))
    private float damage$modifyBeforeAbsorption(final float damage) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null ? damage : tracker.startStep(DamageStepTypes.ABSORPTION, damage, this);
    }

    @WrapWithCondition(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;setReduction(Lnet/neoforged/neoforge/common/damagesource/DamageContainer$Reduction;F)V"), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F")))
    private boolean damage$skipAbsorption(final DamageContainer container, final DamageContainer.Reduction reduction, final float absorbed) {
        final SpongeDamageTracker tracker = this.damage$tracker();
        return tracker == null || !tracker.isSkipped(DamageStepTypes.ABSORPTION);
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("LOAD"), ordinal = 3, slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/Identifier;I)V"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;")))
    private float damage$firePostEvent_Living(final float damage) {
        return this.damage$firePostEvent(damage);
    }
}
