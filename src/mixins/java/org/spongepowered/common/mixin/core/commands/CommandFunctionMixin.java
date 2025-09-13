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
package org.spongepowered.common.mixin.core.commands;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.commands.CommandSourceStackBridge;
import org.spongepowered.common.command.brigadier.dispatcher.DelegatingCommandDispatcher;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.util.List;

@Mixin(CommandFunction.class)
public interface CommandFunctionMixin {

    @WrapMethod(method = "fromLines")
    private static <T extends ExecutionCommandSource<T>> CommandFunction<T> impl$onFromLines(
        final ResourceLocation $$0, final CommandDispatcher<T> $$1, final T $$2, final List<String> $$3, final Operation<CommandFunction<T>> original) {
        if ($$1 instanceof final DelegatingCommandDispatcher delegatingCommandDispatcher) {
            try (final CauseStackManager.StackFrame frame = PhaseTracker.getInstance().pushCauseFrame()) {
                frame.pushCause(delegatingCommandDispatcher.permissionService().newSubjectReference(PermissionService.SUBJECTS_SYSTEM, "console").resolve().join());
                final CommandCause cause = ((CommandSourceStackBridge) $$2).bridge$withCurrentCause();
                ((CommandSourceStackBridge) cause).bridge$permissionService(delegatingCommandDispatcher.permissionService());
                return original.call($$0, $$1, cause, $$3);
            }
        } else {
            return original.call($$0, $$1, $$2, $$3);
        }
    }
}
