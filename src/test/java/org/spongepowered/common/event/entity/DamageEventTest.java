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
package org.spongepowered.common.event.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

public class DamageEventTest {
    private ServerLocation location;

    @BeforeEach
    public void prepare() {
        this.location = Sponge.server().worldManager().world(DefaultWorldKeys.DEFAULT).get().location(Vector3d.ZERO);
    }

    @Test
    public void testDamageEvent() {
        final EventManager eventManager = Sponge.eventManager();
        final PluginContainer plugin = Mockito.mock(PluginContainer.class);
        final DamageEventListener listener = new DamageEventListener();
        final Entity sheep = this.location.createEntity(EntityTypes.SHEEP.get());
        final DamageSource damageSource = DamageSource.builder().type(DamageTypes.CACTUS).build();

        eventManager.registerListeners(plugin, listener);

        sheep.damage(15, damageSource);

        assertEquals(1, listener.events, "events count");
        assertNotNull(listener.lastEvent, "last event");
        assertEquals(15, listener.lastEvent.baseDamage(), "base damage");
        assertEquals(damageSource, listener.lastEvent.cause().root(), "root cause");

        eventManager.unregisterListeners(listener);
    }

    private static class DamageEventListener {
        @MonotonicNonNull DamageEntityEvent lastEvent;
        int events = 0;

        @Listener
        public void onEvent(final DamageEntityEvent event) {
            this.events++;
            this.lastEvent = event;
        }
    }
}
