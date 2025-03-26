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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.util.DamageEventUtil;

import java.util.ArrayList;

@Mixin(LivingEntity.class)
public class LivingEntityMixin_Neo_Attack_Impl {

    protected DamageEventUtil.DamageEventResult attackImpl$actuallyHurtResult;
    protected DamageEventUtil.Hurt attackImpl$hurt;

    /**
     * Prepare {@link org.spongepowered.common.util.DamageEventUtil.Hurt} for damage event
     */
    @Inject(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I"))
    private void attackImpl$neo$preventEarlyBlock1(final ServerLevel level, final DamageSource $$0, final float $$1, final CallbackInfoReturnable<Boolean> cir) {
        this.attackImpl$hurt = new DamageEventUtil.Hurt($$0, new ArrayList<>());
    }

    /**
     * Prevents shield usage before event
     * Captures the blocked damage as a function
     */
    @WrapOperation(method = "applyItemBlocking", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;FI)V"))
    private void attackImpl$preventItemBlocking(
        final BlocksAttacks instance, final Level level, final ItemStack itemStack, final LivingEntity target,
        final InteractionHand interactionHand, final float v, final int i, final Operation<Void> original
    ) {
        // this.hurtCurrentlyUsedShield(damageToShield);
        // $$6.hurtBlockingItem(this.level(), this.getUseItem(), this, this.getUsedItemHand(), $$5);
        this.attackImpl$hurt.functions().add(DamageEventUtil.createShieldFunction(target));
    }

    /**
     * Set absorbed damage after calling {@link LivingEntity#setAbsorptionAmount} in which we called the event
     */
    @ModifyVariable(method = "actuallyHurt", ordinal = 2,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V", shift = At.Shift.AFTER))
    public float attackImpl$setAbsorbed(final float value) {
        if (this.attackImpl$actuallyHurtResult.event().isCancelled()) {
            return 0;
        }
        return this.attackImpl$actuallyHurtResult.damageAbsorbed().orElse(0f);
    }

    /**
     * Prevents {@link ServerPlayer#awardStat} from running before event
     */
    @Redirect(method = "getDamageAfterMagicAbsorb",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/ResourceLocation;I)V"))
    public void attackImpl$onAwardStatDamageResist(final ServerPlayer instance, final ResourceLocation resourceLocation, final int i) {
        // do nothing
    }

}
