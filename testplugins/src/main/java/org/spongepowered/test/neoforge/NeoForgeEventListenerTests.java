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
package org.spongepowered.test.neoforge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Color;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.List;

@Plugin("neoforgeeventtest")
public class NeoForgeEventListenerTests {
    private static final Logger LOGGER = LogManager.getLogger();

    @Listener
    public void changeDispense(DropItemEvent.Dispense event) {
        for (var entity : event.entities()) {
            entity.get(Keys.ITEM_STACK_SNAPSHOT).ifPresent(s -> {
                LOGGER.info("Changing dispense of " + s);
            });
            entity.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.builder()
                .itemType(ItemTypes.NETHERITE_SWORD)
                .add(Keys.APPLIED_ENCHANTMENTS, List.of(
                    Enchantment.of(EnchantmentTypes.SHARPNESS, 1000),
                    Enchantment.of(EnchantmentTypes.LOOTING, 10),
                    Enchantment.of(EnchantmentTypes.BANE_OF_ARTHROPODS, 10)
                ))
                .add(Keys.CUSTOM_NAME, Component.text("[", TextColor.color(Color.GRAY))
                    .append(Component.text("SuperMegaSwordOfDeath", TextColor.color(Color.DARK_CYAN), TextDecoration.BOLD))
                    .append(Component.text("]", TextColor.color(Color.GRAY))))
                .quantity(1)
                .build()
                .asImmutable()
            );
        }
    }
}
