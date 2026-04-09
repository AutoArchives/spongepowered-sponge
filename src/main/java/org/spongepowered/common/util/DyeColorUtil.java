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
package org.spongepowered.common.util;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;

import java.util.HashMap;
import java.util.Map;

public class DyeColorUtil {

    public static final Map<Block, DyeColor> COLOR_BY_WOOL;

    public static final Map<Block, DyeColor> COLOR_BY_TERRACOTTA;

    static {
        final Map<Block, DyeColor> byWool = new HashMap<>();
        final Map<Block, DyeColor> byTerracotta = new HashMap<>();
        final ColorCollection<Block> wools = Blocks.WOOL;
        final ColorCollection<Block> terracottas = Blocks.DYED_TERRACOTTA;
        for (final DyeColor color : DyeColor.values()) {
            byWool.put(wools.pick(color), color);
            byTerracotta.put(terracottas.pick(color), color);
        }
        COLOR_BY_WOOL = Map.copyOf(byWool);
        COLOR_BY_TERRACOTTA = Map.copyOf(byTerracotta);
    }

    private DyeColorUtil() {
    }
}
