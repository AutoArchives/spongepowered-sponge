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
package org.spongepowered.common.mixin.api.minecraft.world.level.storage;

import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.saveddata.WanderingTraderData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.trader.WanderingTrader;
import org.spongepowered.api.util.MinecraftDayTime;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.gamerule.GameRule;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.WeatherType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.server.level.ServerLevelAccessor;
import org.spongepowered.common.accessor.world.entity.npc.wanderingtrader.WanderingTraderSpawnerAccessor;
import org.spongepowered.common.accessor.world.level.GameRulesAccessor;
import org.spongepowered.common.bridge.world.level.storage.ServerLevelDataBridge;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeMinecraftDayTime;
import org.spongepowered.common.util.SpongeTicks;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
@Mixin(ServerLevelData.class)
public interface ServerLevelDataMixin_API extends ServerWorldProperties {

    // @formatter:off
    // @formatter:on

    @Override
    default ResourceKey key() {
        return Objects.requireNonNull(((ServerLevelDataBridge) this).bridge$spongeData().key());
    }

    @Override
    default Optional<ServerWorld> world() {
        return Optional.ofNullable((ServerWorld) ((ServerLevelDataBridge) this).bridge$level());
    }

    @Override
    default String name() {
        return this.key().asString();
    }

    @Override
    default UUID uniqueId() {
        return ((ServerLevelDataBridge) this).bridge$spongeData().uniqueId();
    }

    @Override
    default <V> V gameRule(GameRule<V> gameRule) {
        final var value = ((ServerLevelDataBridge) this).bridge$level().getGameRules().get((net.minecraft.world.level.gamerules.GameRule<?>) (Object) Objects.requireNonNull(gameRule,
            "gameRule"));
        return (V) value;
    }

    @Override
    default <V> void setGameRule(final GameRule<V> gameRule, final V value) {
        Objects.requireNonNull(gameRule, "gameRule");
        Objects.requireNonNull(value, "value");

        ((ServerLevelDataBridge) this).bridge$level().getGameRules().set((net.minecraft.world.level.gamerules.GameRule<V>) (Object) gameRule, value, SpongeCommon.server());
    }

    @Override
    default Map<GameRule<?>, ?> gameRules() {
        final GameRuleMap rules = ((GameRulesAccessor) ((ServerLevelDataBridge) this).bridge$level().getGameRules()).accessor$rules();
        final Map<GameRule<?>, Object> apiRules = new HashMap<>();
        for (final var rule : rules.keySet()) {
            final GameRule<?> key = (GameRule<?>) (Object) rule;
            apiRules.put(key, rules.get(rule));
        }
        return apiRules;
    }

    @Override
    default void setDayTime(final MinecraftDayTime dayTime) {
        // TODO - adjust to world clocks now
//        this.setClockStates(this.clockStates().clocks());
    }

    @SuppressWarnings("removal")
    @Override
    default Ticks wanderingTraderSpawnDelay() {
        final var data = this.api$wanderingTraderData();
        if (data == null) {
            return SpongeTicks.ticksOrInfinite(0);
        }
        return SpongeTicks.ticksOrInfinite(data.spawnDelay());
    }

    @SuppressWarnings("removal")
    @Override
    default void setWanderingTraderSpawnDelay(final Ticks delay) {
        final var data = this.api$wanderingTraderData();
        if (data != null) {
            data.setSpawnDelay((int) delay.ticks());
        }
    }

    @SuppressWarnings("removal")
    @Override
    default int wanderingTraderSpawnChance() {
        final var data = this.api$wanderingTraderData();
        if (data == null) {
            return 0;
        }
        return data.spawnChance();
    }

    @SuppressWarnings("removal")
    @Override
    default void setWanderingTraderSpawnChance(final int chance) {
        final var data = this.api$wanderingTraderData();
        if (data != null) {
            data.setSpawnChance(chance);
        }
    }

    @SuppressWarnings("removal")
    @Override
    default void setWanderingTrader(@Nullable final WanderingTrader trader) {
        // Wandering trader is no longer tracked in WanderingTraderData as of 26.1-snapshot-6
    }

    @SuppressWarnings("removal")
    @Override
    default Optional<UUID> wanderTraderUniqueId() {
        // Wandering trader is no longer tracked in WanderingTraderData as of 26.1-snapshot-6
        return Optional.empty();
    }

    private @Nullable WanderingTraderData api$wanderingTraderData() {
        final var level = ((ServerLevelDataBridge) this).bridge$level();
        for (final CustomSpawner spawner : ((ServerLevelAccessor) level).accessor$customSpawners()) {
            if (spawner instanceof WanderingTraderSpawnerAccessor wtsa) {
                return wtsa.invoker$getTraderData();
            }
        }
        return null;
    }

    @Override
    default void setWeather(final WeatherType type) {
        this.offer(Keys.WEATHER, Weather.of(Objects.requireNonNull(type, "type"), 6000 / Constants.TickConversions.TICK_DURATION_MS));
    }

    @Override
    default void setWeather(final WeatherType type, final Ticks ticks) {
        this.offer(Keys.WEATHER, Weather.of(Objects.requireNonNull(type, "type"), Objects.requireNonNull(ticks, "ticks")));
    }

    @Override
    default MinecraftDayTime dayTime() {
        final var clockstate = ((ServerLevelDataBridge) this).bridge$level().clockManager().packState().clocks().get(WorldClocks.OVERWORLD);
        if (clockstate == null) {
            return new SpongeMinecraftDayTime(0);
        }
        final var totalTicks = clockstate.totalTicks();
        return new SpongeMinecraftDayTime(totalTicks);
    }
}
