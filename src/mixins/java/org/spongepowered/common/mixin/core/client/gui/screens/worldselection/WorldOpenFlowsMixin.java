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
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.bridge.server.ReloadableServerResourcesBridge;
import org.spongepowered.common.bridge.server.packs.resources.MultiPackResourceManagerBridge;
import org.spongepowered.common.bridge.server.packs.resources.ResourceManagerBridge;
import org.spongepowered.common.registry.SpongeRegistryHolder;
import org.spongepowered.common.tag.SpongePluginTags;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    // Since 26.1 this path hands the WorldStem a fresh resource manager instead of the one
    // WorldLoader.load created, so the Sponge state established during datapack loading
    // (server-scoped plugin registries, services, plugin tags) must carry over to it.
    @WrapOperation(method = "createLevelFromExistingSettings",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/WorldLoader$PackConfig;createResourceManager()Lcom/mojang/datafixers/util/Pair;"))
    private Pair<WorldDataConfiguration, CloseableResourceManager> impl$transferSpongeState(
        final WorldLoader.PackConfig config, final Operation<Pair<WorldDataConfiguration, CloseableResourceManager>> original,
        final @Local(argsOnly = true) ReloadableServerResources serverResources
    ) {
        final Pair<WorldDataConfiguration, CloseableResourceManager> pair = original.call(config);
        final @Nullable ResourceManager originalManager = ((ReloadableServerResourcesBridge) serverResources).bridge$resourceManager();
        final CloseableResourceManager newManager = pair.getSecond();
        if (originalManager != null && originalManager != newManager) {
            ((MultiPackResourceManagerBridge) newManager).bridge$setRegistryHolder(((SpongeRegistryHolder) originalManager).registryHolder());
            ((ResourceManagerBridge) newManager).bridge$services(((ResourceManagerBridge) originalManager).bridge$services());
            final @Nullable SpongePluginTags tags = ((ResourceManagerBridge) originalManager).bridge$pluginProvidedTags();
            if (tags != null) {
                ((ResourceManagerBridge) newManager).bridge$pluginProvidedTags(tags);
            }
        }
        return pair;
    }
}
