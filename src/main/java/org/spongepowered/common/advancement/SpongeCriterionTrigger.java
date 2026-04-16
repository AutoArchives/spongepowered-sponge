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
package org.spongepowered.common.advancement;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import io.leangen.geantyref.TypeToken;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.trigger.FilteredTrigger;
import org.spongepowered.api.advancement.criteria.trigger.FilteredTriggerConfiguration;
import org.spongepowered.api.advancement.criteria.trigger.Trigger;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.advancement.CriterionEvent;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.advancements.CriterionTriggerBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public class SpongeCriterionTrigger implements CriterionTrigger<SpongeFilteredTrigger>, CriterionTriggerBridge {

    private final Type triggerConfigurationType;
    final Function<JsonObject, FilteredTriggerConfiguration> constructor;
    final @Nullable Consumer<CriterionEvent.Trigger> eventHandler;
    private final String name;


    @Override
    public Codec<SpongeFilteredTrigger> codec() {
        return Codec.PASSTHROUGH.comapFlatMap(in -> {
                    var json = in.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
                    final FilteredTriggerConfiguration config = this.constructor.apply(json);
                    return DataResult.success(new SpongeFilteredTrigger(config));
                }, trigger -> new Dynamic<>(JsonOps.INSTANCE, trigger.serializeToJson())
        );
    }

    SpongeCriterionTrigger(final Type triggerConfigurationType,
        final Function<JsonObject, FilteredTriggerConfiguration> constructor,
        final @Nullable Consumer<CriterionEvent.Trigger> eventHandler,
        final String name) {
        this.triggerConfigurationType = triggerConfigurationType;
        this.eventHandler = eventHandler;
        this.constructor = constructor;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void bridge$trigger(final ServerPlayer player) {
        final net.minecraft.server.level.ServerPlayer mcPlayer = (net.minecraft.server.level.ServerPlayer) player;
        final PlayerAdvancements playerAdvancements = mcPlayer.getAdvancements();
        final Map<PlayerAdvancements.TriggerInstanceKey, SpongeFilteredTrigger> map = playerAdvancements.getTriggerMapForType(this);
        if (map == null || map.isEmpty()) {
            return;
        }
        final Cause cause = PhaseTracker.getInstance().currentCause();

        @SuppressWarnings("unchecked") // correct type verified in builder
        final TypeToken<FilteredTriggerConfiguration> typeToken = (TypeToken<FilteredTriggerConfiguration>) TypeToken.get(this.triggerConfigurationType);
        for (final var entry : new ArrayList<>(map.entrySet())) {
            final PlayerAdvancements.TriggerInstanceKey key = entry.getKey();
            final SpongeFilteredTrigger triggerInstance = entry.getValue();
            final var advancement = key.advancement().value();
            final var advancementKey = (ResourceKey) (Object) key.advancement().id();
            final AdvancementCriterion advancementCriterion = (AdvancementCriterion) (Object) advancement.criteria().get(key.criterion());
            final CriterionEvent.Trigger event = SpongeEventFactory.createCriterionEventTrigger(cause, (Advancement) (Object) advancement, advancementKey, advancementCriterion,
                typeToken, player, (FilteredTrigger) triggerInstance, (Trigger<FilteredTriggerConfiguration>) advancementCriterion.type().get(), this.eventHandler == null);
            if (this.eventHandler != null) {
                this.eventHandler.accept(event);
                if (!event.result()) {
                    continue;
                }
            }
            SpongeCommon.post(event);
            if (event.result()) {
                playerAdvancements.award(key.advancement(), key.criterion());
            }
        }
    }

    public @org.checkerframework.checker.nullness.qual.Nullable Consumer<CriterionEvent.Trigger> getEventHandler() {
        return this.eventHandler;
    }
}
