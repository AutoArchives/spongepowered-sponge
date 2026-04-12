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
package org.spongepowered.common.hooks;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;

public interface WorldHooks {

    default Entity getCustomEntityIfItem(final Entity entity) {
        return null;
    }

    default boolean isRestoringBlocks(final Level world) {
        return PhaseTracker.getInstance().getPhaseContext().isRestoring();
    }

    default void postLoadWorld(ServerLevel world) { }

    default void preUnloadWorld(ServerLevel world) { }

    default Registry<LevelStem> earlyRegistryAccess(ResourceKey<Registry<LevelStem>> levelStem) {
        // LEVEL_STEM lives in the server's composite registryAccess() but not in Sponge's scoped
        // holder (which excludes DIMENSION_REGISTRIES during RegistryDataLoader processing), so we
        // must bypass SpongeCommon.vanillaRegistry() and hit server().registryAccess() directly.
        return SpongeCommon.server().registryAccess().lookupOrThrow(levelStem);
    }
}
