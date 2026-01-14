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
package org.spongepowered.common.registry.provider;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.world.WorldTypeEffect;
import org.spongepowered.common.world.SpongeWorldTypeEffect;

import java.util.Map;

public final class DimensionEffectProvider {

    public static DimensionEffectProvider INSTANCE = new DimensionEffectProvider();

    private static final SpongeWorldTypeEffect OVERWORLD = new SpongeWorldTypeEffect((ResourceKey) (Object) BuiltinDimensionTypes.OVERWORLD.identifier());
    private static final SpongeWorldTypeEffect NETHER = new SpongeWorldTypeEffect((ResourceKey) (Object) BuiltinDimensionTypes.NETHER.identifier());
    private static final SpongeWorldTypeEffect END = new SpongeWorldTypeEffect((ResourceKey) (Object) BuiltinDimensionTypes.END.identifier());

    static {
        DimensionEffectProvider.INSTANCE.register(DimensionType.Skybox.OVERWORLD, OVERWORLD);
        DimensionEffectProvider.INSTANCE.register(DimensionType.Skybox.NONE, NETHER);
        DimensionEffectProvider.INSTANCE.register(DimensionType.Skybox.END, END);
    }

    private final Map<DimensionType.Skybox, WorldTypeEffect> mappings;

    private DimensionEffectProvider() {
        this.mappings = new Object2ObjectOpenHashMap<>();
    }

    public @Nullable WorldTypeEffect get(final DimensionType.Skybox key) {
        return this.mappings.get(key);
    }

    public void register(final DimensionType.Skybox key, final WorldTypeEffect effect) {
        this.mappings.put(key, effect);
    }
}
