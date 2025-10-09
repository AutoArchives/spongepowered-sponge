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
package org.spongepowered.common.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.data.VanishableBridge;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity")
public abstract class ChunkMap_TrackedEntityMixin {

    @Shadow @Final Entity entity;

    /**
     * @author gabizou
     * @reason Because of the public availability of some methods, a packet
     * being sent for a "vanished" entity is not permissible since the vanished
     * entity is being "removed" from clients by way of literally being mimiced being
     * "untracked". This safeguards the players being updated erroneously.
     */
    @Inject(method = "sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void impl$ignoreVanished(final Packet<?> packet, final CallbackInfo ci) {
        if (this.entity instanceof VanishableBridge) {
            if (((VanishableBridge) this.entity).bridge$vanishState().invisible()) {
                ci.cancel();
            }
        }
    }

    @WrapOperation(
        method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"),
        require = 0 // mod compat
    )
    private boolean impl$isSpectatedOrVanished(final Entity entity, final ServerPlayer player, Operation<Boolean> original) {
        if (entity instanceof VanishableBridge) {
            if (((VanishableBridge) entity).bridge$vanishState().invisible()) {
                return false;
            }
        }
        return original.call(entity, player);
    }

}
