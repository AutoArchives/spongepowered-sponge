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
package org.spongepowered.common.mixin.gametest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.scheduler.SyncScheduler;

import java.util.concurrent.TimeUnit;

/**
 * The game test server ticks without waiting, so a second of wall-clock time spans no particular number of ticks. A
 * plugin's time-based delays and intervals count ticks of the nominal length instead, as its tick-based ones do, and a
 * test can wait for them.
 */
@Mixin(SyncScheduler.class)
public abstract class SyncSchedulerMixin_GameTest {

    private static final long GAMETEST$TICK_NS = TimeUnit.MILLISECONDS.toNanos(50);

    @Shadow private long counter;

    /**
     * @author Sponge
     * @reason Tick-based time on the game test server, see the class javadoc
     */
    @Overwrite
    protected long timestamp(final boolean tickBased) {
        return this.counter * SyncSchedulerMixin_GameTest.GAMETEST$TICK_NS;
    }
}
