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
package org.spongepowered.common.entity.player;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.common.SpongeCommon;

import java.util.Arrays;
import java.util.List;

public class SpongeUserInventory implements Container {

    // sourced from InventoryPlayer

    /** An array of 36 item stacks indicating the main player inventory (including the visible bar). */
    final NonNullList<ItemStack> mainInventory = NonNullList.withSize(36, ItemStack.EMPTY);
    /** An array of 4 item stacks containing the currently worn armor pieces. */
    final NonNullList<ItemStack> armorInventory = NonNullList.withSize(4, ItemStack.EMPTY);
    final NonNullList<ItemStack> offHandInventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private final List<NonNullList<ItemStack>> allInventories;
    /** The index of the currently held item (0-8). */
    public int currentItem;
    /** The player whose inventory this is. */
    private final SpongeUserData userData;
    private boolean dirty = false;

    public SpongeUserInventory(final SpongeUserData userData) {
        this.allInventories = Arrays.asList(this.mainInventory, this.armorInventory, this.offHandInventory);
        this.userData = userData;
    }

    public ItemStack getCurrentItem() {
        return Inventory.isHotbarSlot(this.currentItem) ? this.mainInventory.get(this.currentItem) : ItemStack.EMPTY;
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int index, final int count) {
        this.setChanged();
        List<ItemStack> list = null;

        for (final NonNullList<ItemStack> nonnulllist : this.allInventories) {
            if (index < nonnulllist.size()) {
                list = nonnulllist;
                break;
            }

            index -= nonnulllist.size();
        }

        return list != null && !list.get(index).isEmpty() ? ContainerHelper.removeItem(list, index, count) : ItemStack.EMPTY;
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        this.setChanged();
        NonNullList<ItemStack> nonnulllist = null;

        for (final NonNullList<ItemStack> nonnulllist1 : this.allInventories) {
            if (index < nonnulllist1.size()) {
                nonnulllist = nonnulllist1;
                break;
            }

            index -= nonnulllist1.size();
        }

        if (nonnulllist != null && !nonnulllist.get(index).isEmpty()) {
            final ItemStack itemstack = nonnulllist.get(index);
            nonnulllist.set(index, ItemStack.EMPTY);
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int index, final ItemStack stack) {
        this.setChanged();
        NonNullList<ItemStack> nonnulllist = null;

        for (final NonNullList<ItemStack> nonnulllist1 : this.allInventories) {
            if (index < nonnulllist1.size()) {
                nonnulllist = nonnulllist1;
                break;
            }

            index -= nonnulllist1.size();
        }

        if (nonnulllist != null) {
            nonnulllist.set(index, stack);
        }
    }

    /**
     * Writes the inventory out as a list of compound tags. This is where the slot indices are used (+100 for armor, +80
     * for crafting).
     */
    public void writeList(final ValueOutput.TypedOutputList<ItemStackWithSlot> nbtTagListIn) {
        for (int i = 0; i < this.mainInventory.size(); ++i) {
            final var stack = this.mainInventory.get(i);
            if (!stack.isEmpty()) {
                final var slottedItem = new ItemStackWithSlot(i, stack);
                nbtTagListIn.add(slottedItem);
            }
        }

        for (int j = 0; j < this.armorInventory.size(); ++j) {
            final var armorItem = this.armorInventory.get(j);
            if (!armorItem.isEmpty()) {
                final CompoundTag nbttagcompound1 = new CompoundTag();
                nbttagcompound1.putByte("Slot", (byte) (j + 100));
                final var slottedItem = new ItemStackWithSlot(j + 100, armorItem);
                nbtTagListIn.add(slottedItem);
            }
        }

        for (int k = 0; k < this.offHandInventory.size(); ++k) {
            final var offhandItem = this.offHandInventory.get(k);
            if (!offhandItem.isEmpty()) {
                final CompoundTag nbttagcompound2 = new CompoundTag();
                nbttagcompound2.putByte("Slot", (byte) (k + 150));
                nbtTagListIn.add(new ItemStackWithSlot(k + 150, offhandItem));
            }
        }

        this.dirty = false;
    }

    /**
     * Reads from the given tag list and fills the slots in the inventory with the correct items.
     */
    public void readList(final ValueInput.TypedInputList<ItemStackWithSlot> nbtTagListIn) {
        this.mainInventory.clear();
        this.armorInventory.clear();
        this.offHandInventory.clear();

        for (final var input : nbtTagListIn) {
            final var slot = input.slot();
            if (slot >= 0 && slot < this.mainInventory.size()) {
                this.mainInventory.set(slot, input.stack());
            } else if (slot >= 100 && slot < this.armorInventory.size() + 100) {
                this.armorInventory.set(slot - 100, input.stack());
            } else if (slot >= 150 && slot < this.offHandInventory.size() + 150) {
                this.offHandInventory.set(slot - 150, input.stack());
            } else {
                SpongeCommon.logger().warn("Skipping Slot {} as it does not exist in the inventory", slot);
            }
        }
    }

    /**
     * Returns the number of slots in the inventory.
     */
    @Override
    public int getContainerSize() {
        return this.mainInventory.size() + this.armorInventory.size() + this.offHandInventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack itemstack : this.mainInventory) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        for (final ItemStack itemstack1 : this.armorInventory) {
            if (!itemstack1.isEmpty()) {
                return false;
            }
        }

        for (final ItemStack itemstack2 : this.offHandInventory) {
            if (!itemstack2.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the stack in the given slot.
     */
    @Override
    public ItemStack getItem(int index) {
        List<ItemStack> list = null;

        for (final NonNullList<ItemStack> nonnulllist : this.allInventories) {
            if (index < nonnulllist.size()) {
                list = nonnulllist;
                break;
            }

            index -= nonnulllist.size();
        }

        return list == null ? ItemStack.EMPTY : list.get(index);
    }

    /**
     * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it
     * hasn't changed and skip it.
     */
    @Override
    public void setChanged() {
        this.dirty = true;
        this.userData.markDirty();
    }

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (final List<ItemStack> list : this.allInventories) {
            list.clear();
        }
    }

}
