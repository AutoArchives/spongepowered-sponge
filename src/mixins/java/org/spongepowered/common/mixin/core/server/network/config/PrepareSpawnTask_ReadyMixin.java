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
package org.spongepowered.common.mixin.core.server.network.config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Ready")
public class PrepareSpawnTask_ReadyMixin {

    @WrapOperation(
        method = "spawn", // or the mapped method containing the Player.load call
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"
            // If your version uses CompoundTag instead, use the proper descriptor
        )
    )
    private void impl$afterPlayerLoad(
        final Optional<ValueInput> instance, final Consumer<? super ValueInput> action,
        final Operation<Void> original,
        @Local net.minecraft.server.level.ServerPlayer player
        ) throws IOException {
        // Call vanilla first
        original.call(instance, action);

        final ServerPlayer sPlayer = (ServerPlayer) player;
        if (sPlayer.get(Keys.FIRST_DATE_JOINED).isEmpty()) {
            // TODO - 25w32a changed where we have access to the Player and moved access to the data files elsewhere.
//            final Path file = new File(this.playerDir, player.getStringUUID() + ".dat").toPath();
//            final Instant creationTime = java.nio.file.Files.exists(file)
//                ? java.nio.file.Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class)
//                .creationTime().toInstant()
//                : null;
//
//            ((SpongeServer) SpongeCommon.server())
//                .getPlayerDataManager()
//                .readLegacyPlayerData(sPlayer, nbtOrValueInput, creationTime);
        }

        sPlayer.offer(Keys.LAST_DATE_JOINED, java.time.Instant.now());
    }


    @WrapOperation(method = "spawn",
        at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;loadPlayerData(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;snapTo(Lnet/minecraft/world/phys/Vec3;FF)V")
        )
    )
    private void impl$setPlayerDataForNewPlayers(
        final Optional<ValueInput> instance, final Consumer<? super ValueInput> action,
        final Operation<Void> original,
        @Local final net.minecraft.server.level.ServerPlayer playerIn
    ) {
        original.call(instance, action);
        if (instance.isEmpty()) {
            final Instant now = Instant.now();
            ((ServerPlayer) playerIn).offer(Keys.FIRST_DATE_JOINED, now);
            ((ServerPlayer) playerIn).offer(Keys.LAST_DATE_JOINED, now);
        }
    }
}
