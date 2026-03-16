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
package org.spongepowered.common.mixin.core.server.commands;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherCommand.class)
public abstract class WeatherCommandMixin {

    @WrapOperation(
        method = "getDuration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRandom()Lnet/minecraft/util/RandomSource;")
    )
    private static RandomSource impl$useCurrentWorld(final ServerLevel instance, final Operation<RandomSource> original, final CommandSourceStack source) {
        return original.call(source.getLevel());
    }

    /**
     * Redirects weather parameter changes to the command source's current world
     * instead of the global server weather. This enables per-world weather control
     * via the {@code /weather} command.
     *
     * <p>The vanilla implementation calls {@link MinecraftServer#setWeatherParameters(int, int, boolean, boolean)}
     * which sets weather on the global {@link WeatherData}. Since {@code ServerLevel} does not extend
     * {@code MinecraftServer}, we cannot pass the level to {@code original.call()}. Instead, we
     * directly manipulate the per-world {@link WeatherData} obtained from the source's level.</p>
     */
    @WrapOperation(method = {
        "setClear",
        "setRain",
        "setThunder"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setWeatherParameters(IIZZ)V"))
    private static void impl$useCurrentWorld(
        final MinecraftServer instance, final int clearTime, final int rainTime, final boolean raining,
        final boolean thundering, final Operation<Void> original,
        final CommandSourceStack source
    ) {
        final WeatherData weatherData = source.getLevel().getWeatherData();
        weatherData.setClearWeatherTime(clearTime);
        weatherData.setRainTime(rainTime);
        weatherData.setThunderTime(rainTime);
        weatherData.setRaining(raining);
        weatherData.setThundering(thundering);
    }
}
