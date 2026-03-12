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
package org.spongepowered.common.mixin.tracker.world.level.dimension.end;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.generation.FeaturePhaseContext;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.event.tracking.phase.world.dragon.DragonPhase;
import org.spongepowered.common.event.tracking.phase.world.dragon.SpawnDragonContext;

import java.util.List;

@Mixin(EnderDragonFight.class)
public abstract class EndDragonFightMixin_Tracker {

    // @formatter:off
    @Shadow private ServerLevel level;
    // @formatter:on

    @Redirect(method = "lambda$spawnNewGateway$1",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean tracker$switchToFeatureState(
        final ConfiguredFeature<?, ?> configuredFeature, final WorldGenLevel worldIn, final ChunkGenerator generator,
        final RandomSource rand, final BlockPos pos
    ) {

        try (final FeaturePhaseContext context = GenerationPhase.State.FEATURE_PLACEMENT.createPhaseContext(PhaseTracker.getWorldInstance(worldIn.getLevel()))) {
            context
                .world((ServerLevel) worldIn)
                .generator(generator)
                .feature(configuredFeature.feature())
                .origin(pos)
            ;
            context.buildAndSwitch();

            return configuredFeature.place(worldIn, generator, rand, pos);
        }
    }

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/end/DragonRespawnStage;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/dimension/end/EnderDragonFight;Ljava/util/List;I)V"))
    private void tracker$switchToSpawnDragonState(
        final DragonRespawnStage instance, final ServerLevel serverLevel, final EnderDragonFight manager,
        final List<EndCrystal> endCrystals, int i, final Operation<Void> original
    ) {
        try (final SpawnDragonContext context = DragonPhase.State.SPAWN_DRAGON.createPhaseContext(PhaseTracker.getWorldInstance(serverLevel))) {
            context
                .manager(manager)
                .setIsRespawn(true)
                .buildAndSwitch()
            ;
            ++i;
            original.call(instance, serverLevel, manager, endCrystals, i);
        }
    }

    @WrapOperation(method = "findOrCreateDragon", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;createNewDragon()Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;"))
    private EnderDragon tracker$switchToSpawnDragonState(final EnderDragonFight manager, final Operation<EnderDragon> original) {
        try (final SpawnDragonContext context = DragonPhase.State.SPAWN_DRAGON.createPhaseContext(PhaseTracker.getWorldInstance(this.level))) {
            context
                .manager(manager)
                .setIsRespawn(false)
                .buildAndSwitch()
            ;

            return original.call(manager);
        }
    }
}
