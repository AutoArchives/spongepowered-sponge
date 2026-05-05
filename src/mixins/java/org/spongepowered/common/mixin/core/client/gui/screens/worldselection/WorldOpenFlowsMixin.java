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
package org.spongepowered.common.mixin.core.client.gui.screens.worldselection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.launch.Launch;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    /**
     * The integrated-server "create new world" path constructs a fresh
     * {@link CloseableResourceManager} via {@link WorldLoader.PackConfig#createResourceManager()}
     * outside of {@link WorldLoader#load} — bypassing {@code WorldLoaderMixin}'s
     * {@code establishServerServices} hook. Without it the resource manager that ends up
     * in the {@link net.minecraft.server.WorldStem} has no service provider attached,
     * and {@code MinecraftServerMixin#impl$onInit} stores {@code null} into the server's
     * {@code impl$serviceProvider}, which subsequently NPEs at login time.
     */
    @WrapOperation(method = "createLevelFromExistingSettings",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/WorldLoader$PackConfig;createResourceManager()Lcom/mojang/datafixers/util/Pair;"))
    private Pair<WorldDataConfiguration, CloseableResourceManager> impl$establishIntegratedServerServices(
        final WorldLoader.PackConfig packConfig, final Operation<Pair<WorldDataConfiguration, CloseableResourceManager>> original
    ) {
        final Pair<WorldDataConfiguration, CloseableResourceManager> pair = original.call(packConfig);
        Launch.instance().lifecycle().establishServerServices(pair.getSecond(), PermissionLevel.ALL);
        return pair;
    }
}
