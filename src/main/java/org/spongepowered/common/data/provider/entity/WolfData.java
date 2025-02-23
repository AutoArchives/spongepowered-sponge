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
package org.spongepowered.common.data.provider.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.accessor.world.entity.animal.wolf.WolfAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;

public final class WolfData {

    private WolfData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Wolf.class)
                    .create(Keys.IS_BEGGING_FOR_FOOD)
                        .get(Wolf::isInterested)
                        .set(Wolf::setIsInterested)
                    .create(Keys.IS_WET)
                        .get(h -> ((WolfAccessor) h).accessor$isWet() || ((WolfAccessor) h).accessor$isShaking())
                        .set((h, v) -> {
                            final WolfAccessor accessor = (WolfAccessor) h;
                            accessor.accessor$isWet(v);
                            accessor.accessor$isShaking(v);
                            accessor.accessor$shakeAnim(0f);
                            accessor.accessor$shakeAnimO(0f);
                        });

        // @formatter:on
        final var wolf = registrator.asMutable(Wolf.class);
        final var providers = EntityDataProviders.of(
            EntityDataProviders.holderOf(Keys.WOLF_VARIANT, DataComponents.WOLF_VARIANT, Registries.WOLF_VARIANT),
            EntityDataProviders.holderOf(Keys.WOLF_SOUND_VARIANT, DataComponents.WOLF_SOUND_VARIANT, Registries.WOLF_SOUND_VARIANT),
            EntityDataProviders.enumOf(Keys.DYE_COLOR, DataComponents.WOLF_COLLAR)
        );
        for (var provider : providers) {
            provider.applyToRegistrator(wolf);
        }
    }

}
