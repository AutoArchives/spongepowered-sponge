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
package org.spongepowered.common.entity.avatar;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.common.accessor.world.entity.AvatarAccessor;
import org.spongepowered.common.accessor.world.entity.LivingEntityAccessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MobAvatar extends PathfinderMob implements RangedAttackMob {
    public static final HumanoidArm DEFAULT_MAIN_HAND = Avatar.DEFAULT_MAIN_HAND;

    protected static final EntityDimensions STANDING_DIMENSIONS = AvatarAccessor.accessor$STANDING_DIMENSIONS();
    protected static final Map<Pose, EntityDimensions> POSES = AvatarAccessor.accessor$POSES();

    protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(MobAvatar.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(MobAvatar.class, EntityDataSerializers.BYTE);

    public static void addAttributes(final AttributeSupplier.Builder builder) {
        builder
            // Mob
            .add(Attributes.FOLLOW_RANGE, 16.0F)
            // Player
            .add(Attributes.ATTACK_DAMAGE, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.1F)
            .add(Attributes.ATTACK_SPEED)
            .add(Attributes.SWEEPING_DAMAGE_RATIO)
            .build();
    }

    private boolean noAi, leftHanded, aggressive;

    protected MobAvatar(final EntityType<? extends PathfinderMob> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        // LivingEntity
        builder.define(LivingEntityAccessor.accessor$DATA_LIVING_ENTITY_FLAGS(), (byte) 0);
        builder.define(LivingEntityAccessor.accessor$DATA_HEALTH_ID(), 1.0F);
        builder.define(LivingEntityAccessor.accessor$DATA_EFFECT_PARTICLES(), List.of());
        builder.define(LivingEntityAccessor.accessor$DATA_EFFECT_AMBIENCE_ID(), Boolean.FALSE);
        builder.define(LivingEntityAccessor.accessor$DATA_ARROW_COUNT_ID(), 0);
        builder.define(LivingEntityAccessor.accessor$DATA_STINGER_COUNT_ID(), 0);
        builder.define(LivingEntityAccessor.accessor$SLEEPING_POS_ID(), Optional.empty());

        // Mob
        // Skip Mob#DATA_MOB_FLAGS_ID

        // Avatar
        builder.define(MobAvatar.DATA_PLAYER_MAIN_HAND, (byte) Avatar.DEFAULT_MAIN_HAND.getId());
        builder.define(MobAvatar.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0);
    }

    // start: Override all setters and getters that rely on Mob#DATA_MOB_FLAGS_ID
    @Override
    public void setNoAi(boolean value) {
        this.noAi = value;
    }

    @Override
    public void setLeftHanded(boolean value) {
        this.leftHanded = value;
    }

    @Override
    public void setAggressive(boolean value) {
        this.aggressive = value;
    }

    @Override
    public boolean isNoAi() {
        return this.noAi;
    }

    @Override
    public boolean isLeftHanded() {
        return this.leftHanded;
    }

    @Override
    public boolean isAggressive() {
        return this.aggressive;
    }
    // end

    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        final ItemStack projectileStack = this.getItemInHand(InteractionHand.OFF_HAND);
        final ItemStack weaponStack = this.getWeaponItem();
        final Arrow arrow = new Arrow(this.level(), this, projectileStack.getItem() instanceof ArrowItem ? projectileStack : new ItemStack(Items.ARROW), weaponStack);

        final double dx = target.getX() - this.getX();
        final double dy = target.getY(0.3333333333333333) - arrow.getY();
        final double dz = target.getZ() - this.getZ();
        final double dxz = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + dxz * 0.2F, dz, 1.6F, 14 - this.level().getDifficulty().getId() * 4);

        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    // Avatar
    @Override
    public HumanoidArm getMainArm() {
        return this.entityData.get(MobAvatar.DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public void setMainArm(final HumanoidArm arm) {
        this.entityData.set(MobAvatar.DATA_PLAYER_MAIN_HAND, (byte) (arm == HumanoidArm.LEFT ? 0 : 1));
    }

    public boolean isModelPartShown(final PlayerModelPart part) {
        return (this.getEntityData().get(MobAvatar.DATA_PLAYER_MODE_CUSTOMISATION) & part.getMask()) == part.getMask();
    }

    @Override
    public EntityDimensions getDefaultDimensions(final Pose pose) {
        return MobAvatar.POSES.getOrDefault(pose, MobAvatar.STANDING_DIMENSIONS);
    }
}
