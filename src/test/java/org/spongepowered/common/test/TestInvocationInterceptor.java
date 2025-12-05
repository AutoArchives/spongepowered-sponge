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
package org.spongepowered.common.test;

import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.spongepowered.api.Sponge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class TestInvocationInterceptor implements InvocationInterceptor {

    @Override
    public <T> T interceptTestClassConstructor(final Invocation<T> invocation, final ReflectiveInvocationContext<Constructor<T>> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        return this.intercept(invocation);
    }

    @Override
    public void interceptBeforeAllMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    @Override
    public void interceptBeforeEachMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    @Override
    public void interceptTestMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    @Override
    public <T> T interceptTestFactoryMethod(final Invocation<T> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        return this.intercept(invocation);
    }

    @Override
    public void interceptTestTemplateMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    @Override
    public void interceptDynamicTest(final Invocation<Void> invocation, final DynamicTestInvocationContext invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    @Override
    public void interceptAfterAllMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    @Override
    public void interceptAfterEachMethod(final Invocation<Void> invocation, final ReflectiveInvocationContext<Method> invocationContext, final ExtensionContext extensionContext) throws Throwable {
        this.intercept(invocation);
    }

    private <T> T intercept(final Invocation<T> invocation) throws Throwable {
        try (
            final PhaseContext<@NonNull ?> context = TickPhase.Tick.SERVER_TICK
                .createPhaseContext(PhaseTracker.getServerInstanceExplicitly())
                .server((MinecraftServer) Sponge.server())
        ) {
            context.buildAndSwitch();
            return invocation.proceed();
        } finally {
            PhaseTracker.getServerInstanceExplicitly().ensureEmpty();
        }
    }
}
