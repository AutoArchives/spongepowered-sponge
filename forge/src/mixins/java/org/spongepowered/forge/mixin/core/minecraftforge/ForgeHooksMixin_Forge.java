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
package org.spongepowered.forge.mixin.core.minecraftforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.loot.LootModifierManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;

@Mixin(ForgeHooks.class)
public abstract class ForgeHooksMixin_Forge {

    // Forge's LootModifierManager is initialized during resource reload (AddReloadListenerEvent).
    // On an integrated server's first tick, resources may not have loaded yet,
    // causing ForgeInternalHandler.getLootModifierManager() to throw ISE.
    // Guard by wrapping the call and returning an empty modifier list if unavailable.
    @WrapOperation(method = "modifyLoot", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/common/ForgeInternalHandler;getLootModifierManager()Lnet/minecraftforge/common/loot/LootModifierManager;",
        remap = false), remap = false)
    private static LootModifierManager forge$guardLootModifierManager(final Operation<LootModifierManager> original) {
        try {
            return original.call();
        } catch (final IllegalStateException e) {
            return null;
        }
    }

    @WrapOperation(method = "modifyLoot", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/common/loot/LootModifierManager;getAllLootMods()Ljava/util/Collection;",
        remap = false), remap = false)
    private static java.util.Collection<?> forge$guardGetAllLootMods(final LootModifierManager instance, final Operation<java.util.Collection<?>> original) {
        if (instance == null) {
            return Collections.emptyList();
        }
        return original.call(instance);
    }
}
