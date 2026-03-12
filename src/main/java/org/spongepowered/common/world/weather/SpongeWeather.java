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
package org.spongepowered.common.world.weather;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.WeatherType;
import org.spongepowered.api.world.weather.WeatherTypes;
import org.spongepowered.common.bridge.world.level.storage.ServerLevelDataBridge;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;

public final class SpongeWeather implements Weather {

    private static final RandomSource RANDOM = RandomSource.create();

    private final SpongeWeatherType type;
    private final Ticks remainingDuration, runningDuration;

    public SpongeWeather(final SpongeWeatherType type, final Ticks remainingDuration, final Ticks runningDuration) {
        this.type = type;
        this.remainingDuration = remainingDuration;
        this.runningDuration = runningDuration;
    }

    public static Weather of(final WeatherData weatherData) {
        final boolean thundering = weatherData.isThundering();
        if (thundering) {
            final Ticks thunderTime = SpongeTicks.ticksOrInfinite(weatherData.getThunderTime());
            return new SpongeWeather((SpongeWeatherType) WeatherTypes.THUNDER.get(),
                    thunderTime,
                    thunderTime.isInfinite()
                            ? thunderTime
                            : new SpongeTicks(ServerLevel.THUNDER_DURATION.sample(RANDOM)));
        }
        final boolean raining = weatherData.isRaining();
        if (raining) {
            final Ticks rainTime = SpongeTicks.ticksOrInfinite(weatherData.getRainTime());
            return new SpongeWeather((SpongeWeatherType) WeatherTypes.RAIN.get(),
                    rainTime,
                    rainTime.isInfinite()
                            ? rainTime
                            : new SpongeTicks(ServerLevel.RAIN_DURATION.sample(RANDOM)));
        }
        final Ticks clearWeatherTime = SpongeTicks.ticksOrInfinite(weatherData.getClearWeatherTime());
        return new SpongeWeather((SpongeWeatherType) WeatherTypes.CLEAR.get(),
                clearWeatherTime,
                clearWeatherTime.isInfinite()
                        ? clearWeatherTime
                        : new SpongeTicks(ServerLevel.RAIN_DURATION.sample(RANDOM) + ServerLevel.THUNDER_DURATION.sample(RANDOM)));
    }

    public static void apply(final ServerLevelDataBridge levelData, final Weather weather) {
        final var weatherData = levelData.bridge$level().getWeatherData();
        final int time = SpongeTicks.toSaturatedIntOrInfinite(weather.remainingDuration());
        final WeatherType type = weather.type();
        if (type == WeatherTypes.CLEAR.get()) {
            weatherData.setClearWeatherTime(time);
            weatherData.setRaining(false);
            weatherData.setRainTime(0);
            weatherData.setThundering(false);
            weatherData.setThunderTime(0);
        } else if (type == WeatherTypes.RAIN.get()) {
            weatherData.setRaining(true);
            weatherData.setRainTime(time);
            weatherData.setThundering(false);
            weatherData.setThunderTime(0);
            weatherData.setClearWeatherTime(0);
        } else if (type == WeatherTypes.THUNDER.get()) {
            weatherData.setRaining(true);
            weatherData.setRainTime(time);
            weatherData.setThundering(true);
            weatherData.setThunderTime(time);
            weatherData.setClearWeatherTime(0);
        }
    }

    @Override
    public WeatherType type() {
        return this.type;
    }

    @Override
    public Ticks remainingDuration() {
        return this.remainingDuration;
    }

    @Override
    public Ticks runningDuration() {
        return this.runningDuration;
    }

    @Override
    public int contentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew()
                .set(Queries.CONTENT_VERSION, this.contentVersion())
                .set(Constants.Universe.Weather.TYPE, this.type.key(RegistryTypes.WEATHER_TYPE))
                .set(Constants.Universe.Weather.REMAINING_DURATION, this.remainingDuration.ticks())
                .set(Constants.Universe.Weather.RUNNING_DURATION, this.runningDuration.ticks());
    }

    public static class FactoryImpl implements Weather.Factory {

        @Override
        public Weather of(WeatherType type, Ticks remainingDuration, Ticks runningDuration) {
            return new SpongeWeather((SpongeWeatherType) type, remainingDuration, runningDuration);
        }
    }
}
