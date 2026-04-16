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
package org.spongepowered.common.mixin.core.advancements;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.leangen.geantyref.TypeToken;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.ScoreAdvancementCriterion;
import org.spongepowered.api.advancement.criteria.ScoreCriterionProgress;
import org.spongepowered.api.advancement.criteria.trigger.FilteredTrigger;
import org.spongepowered.api.advancement.criteria.trigger.FilteredTriggerConfiguration;
import org.spongepowered.api.advancement.criteria.trigger.Trigger;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.advancement.CriterionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.advancement.SpongeCriterionTrigger;
import org.spongepowered.common.advancement.SpongeFilteredTrigger;
import org.spongepowered.common.bridge.advancements.AdvancementProgressBridge;
import org.spongepowered.common.bridge.advancements.CriterionBridge;
import org.spongepowered.common.bridge.server.PlayerAdvancementsBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.hooks.PlatformHooks;

@Mixin(SimpleCriterionTrigger.class)
public abstract class SimpleCriterionTriggerMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(method = "trigger",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerAdvancements;award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z"))
    private boolean impl$dispatchCriterionEventThenAward(final PlayerAdvancements playerAdvancements,
        final AdvancementHolder holder, final String criterionName, final Operation<Boolean> original) {
        final org.spongepowered.api.advancement.Advancement advancement = (org.spongepowered.api.advancement.Advancement) (Object) holder.value();
        AdvancementCriterion advancementCriterion = (AdvancementCriterion) (Object) holder.value().criteria().get(criterionName);
        final CriterionBridge criterionBridge = (CriterionBridge) advancementCriterion;
        if (criterionBridge.bridge$getScoreCriterion() != null) {
            advancementCriterion = criterionBridge.bridge$getScoreCriterion();
        }

        if (!PlatformHooks.INSTANCE.getGeneralHooks().onServerThread()) {
            // Some mods do advancement granting on async threads — don't post events from here, but still allow the award.
            return original.call(playerAdvancements, holder, criterionName);
        }

        final ServerPlayer player = ((PlayerAdvancementsBridge) playerAdvancements).bridge$getPlayer();
        final CriterionTriggerInstance instance = holder.value().criteria().get(criterionName).triggerInstance();

        // Sponge filters are handled in SpongeCriterionTrigger.bridge$trigger; only post here for vanilla/mod triggers.
        if (!(instance instanceof SpongeFilteredTrigger)) {
            final FilteredTrigger<FilteredTriggerConfiguration> filteredTrigger = (FilteredTrigger) instance;
            final Trigger<?> triggerType = advancementCriterion.type().orElse(null);
            if (triggerType instanceof SpongeCriterionTrigger) {
                final Cause cause = PhaseTracker.getInstance().currentCause();
                final TypeToken<FilteredTriggerConfiguration> typeToken = (TypeToken) TypeToken.get(triggerType.configurationType());
                final CriterionEvent.Trigger event = SpongeEventFactory.createCriterionEventTrigger(cause,
                    advancement, (ResourceKey) (Object) holder.id(), advancementCriterion, typeToken, player, filteredTrigger,
                    (Trigger<FilteredTriggerConfiguration>) advancementCriterion.type().get(), true);
                SpongeCommon.post(event);
                if (!event.result()) {
                    return false;
                }
            }
        }

        PhaseTracker.getInstance().pushCause(instance);
        try {
            // Score criteria absorb the award and increment progress instead.
            if (advancementCriterion instanceof ScoreAdvancementCriterion sac) {
                final AdvancementProgress progress = playerAdvancements.getOrStartProgress(holder);
                final var progressMap = ((AdvancementProgressBridge) progress).bridge$getProgressMap();
                if (progressMap.get(sac.name()) instanceof ScoreCriterionProgress score) {
                    score.add(1);
                }
                return false;
            }
            return original.call(playerAdvancements, holder, criterionName);
        } finally {
            PhaseTracker.getInstance().popCause();
        }
    }
}
