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
package org.spongepowered.common.event.tracking.phase.general;


import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Preconditions;
import org.spongepowered.common.util.VecHelper;

public class RecipeContext extends PhaseContext<RecipeContext> {

    protected PhaseContext<?> priorContext;
    ItemStackSnapshot usedItem;
    Level world;
    BlockState blockState;
    BlockPos pos;
    SpongeBlockSnapshot snapshot;
    SingleItemRecipe recipe;
    Identifier recipeID;
    AbstractFurnaceBlockEntity furnace;

    protected RecipeContext(final IPhaseState<RecipeContext> state, final PhaseTracker tracker) {
        super(state, tracker);
    }

    public RecipeContext provideItem(final ItemStack stack) {
        this.usedItem = ItemStackUtil.snapshotOf(stack);
        return this;
    }

    public ItemStackSnapshot usedItem() {
        return this.usedItem;
    }

    public RecipeContext world(final Level worldIn) {
        this.world = worldIn;
        return this;
    }

    public RecipeContext block(final BlockState blockState) {
        this.blockState = blockState;
        return this;
    }

    public RecipeContext pos(final BlockPos pos) {
        this.pos = pos;
        return this;
    }

    @Override
    public RecipeContext buildAndSwitch() {
        Preconditions.checkState(this.pos != null, "BlockPos is null");
        Preconditions.checkState(this.blockState != null, "BlockState is null");
        if (this.usedItem == null) {
            this.usedItem = ItemStackSnapshot.empty(); // No used item when growing naturally
        }
        Preconditions.checkState(this.world != null, "World is null");
        this.priorContext = this.createdTracker.getPhaseContext();
        Preconditions.checkState(this.priorContext != null, "Prior context is null");
        final SpongeBlockSnapshot.BuilderImpl builder = SpongeBlockSnapshot.BuilderImpl.pooled()
            .world(((ServerLevel) this.world))
            .position(VecHelper.toVector3i(this.pos))
            .blockState(this.blockState)
            .flag(BlockChangeFlags.NONE.withPhysics(true).withUpdateNeighbors(true).withNotifyObservers(true));
        this.priorContext.applyOwnerIfAvailable(builder::creator);
        this.priorContext.applyNotifierIfAvailable(builder::notifier);
        this.snapshot = builder.build();
        return super.buildAndSwitch();
    }

    @Override
    protected void reset() {
        super.reset();
        this.priorContext = null;
        this.usedItem = null;
        this.world = null;
        this.blockState = null;
        this.pos = null;
        this.snapshot = null;
        this.furnace = null;
        this.recipeID = null;
        this.recipe = null;
    }

    @Override
    protected RecipeContext defensiveCopy(PhaseTracker tracker) {
        final RecipeContext newCopy = super.defensiveCopy(tracker);
        return newCopy;
    }

    public SingleItemRecipe recipe() {
        return this.recipe;
    }

    public RecipeContext recipe(SingleItemRecipe value) {
        this.recipe = value;
        return this;
    }

    public RecipeContext furnace(AbstractFurnaceBlockEntity entityIn) {
        this.world = entityIn.getLevel();
        this.pos = entityIn.getBlockPos();
        this.blockState = entityIn.getBlockState();
        this.furnace = entityIn;
        return this;
    }

    public RecipeContext recipeID(Identifier identifier) {
        this.recipeID = identifier;
        return this;
    }

    public Identifier recipeID() {
        return recipeID;
    }

    public AbstractFurnaceBlockEntity furnace() {
        return furnace;
    }
}
