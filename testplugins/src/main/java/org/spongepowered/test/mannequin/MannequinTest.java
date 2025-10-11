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
package org.spongepowered.test.mannequin;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Mannequin;
import org.spongepowered.api.entity.ai.goal.GoalExecutor;
import org.spongepowered.api.entity.ai.goal.GoalExecutorTypes;
import org.spongepowered.api.entity.ai.goal.builtin.LookAtGoal;
import org.spongepowered.api.entity.ai.goal.builtin.LookRandomlyGoal;
import org.spongepowered.api.entity.ai.goal.builtin.SwimGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.AttackLivingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RandomWalkingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RangedAttackAgainstAgentGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.target.FindNearestAttackableTargetGoal;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Monster;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("mannequintest")
public final class MannequinTest {

    private final PluginContainer plugin;

    @Inject
    public MannequinTest(final PluginContainer plugin) {
        this.plugin = plugin;
    }

    @Listener
    private void onRegisterCommand(final RegisterCommandEvent<Command.Parameterized> event) {
        event.register(this.plugin, this.createCommand(false), "warrior");
        event.register(this.plugin, this.createCommand(true), "archer");
    }

    private Command.Parameterized createCommand(final boolean archer) {
        final Parameter.Value<String> nameParameter = Parameter.string().key("name").optional().build();
        final Parameter.Value<String> skinNameParameter = Parameter.string().key("skin_name").optional().build();

        return Command.builder()
            .addParameter(nameParameter)
            .addParameter(skinNameParameter)
            .permission(this.plugin.metadata().id() + ".command.mannequin.create")
            .executor(context -> {
                final ServerPlayer player = context.cause().first(ServerPlayer.class).get();
                final String name = context.one(nameParameter).orElse(player.name());
                final String skinName = context.one(skinNameParameter).orElse(name);

                final Mannequin mannequin = player.world().createEntity(EntityTypes.MANNEQUIN.get(), player.position());
                mannequin.offer(Keys.CUSTOM_NAME, Component.text(name));
                // mannequin.useSkinFor(skinName);

                if (!player.world().spawnEntity(mannequin)) {
                    return CommandResult.error(Component.text("Failed to spawn the mannequin!"));
                }

                this.initHumanEquipment(mannequin, archer);
                this.initHumanGoals(mannequin, archer);

                return CommandResult.success();
            })
            .build();
    }

    public void initHumanGoals(final Mannequin mannequin, final boolean archer) {
        final GoalExecutor<Agent> targetGoal = mannequin.goal(GoalExecutorTypes.TARGET.get()).orElse(null);
        //targetGoal.addGoal(0, FindNearestAttackableTargetGoal.builder().chance(1).target(ServerPlayer.class).build(mannequin));
        targetGoal.addGoal(0, FindNearestAttackableTargetGoal.builder().chance(1).target(Monster.class).build(mannequin));
        targetGoal.addGoal(1, FindNearestAttackableTargetGoal.builder().chance(1).target(Mannequin.class).build(mannequin));

        final GoalExecutor<Agent> normalGoal = mannequin.goal(GoalExecutorTypes.NORMAL.get()).orElse(null);
        normalGoal.addGoal(0, SwimGoal.builder().swimChance(0.8f).build(mannequin));
        //normalGoal.addGoal(0, AvoidLivingGoal.builder().targetSelector(l -> l instanceof Creeper).searchDistance(5).closeRangeSpeed(7).farRangeSpeed(2).build(mannequin));
        if (archer) {
            normalGoal.addGoal(1, RangedAttackAgainstAgentGoal.builder().build(mannequin));
        } else {
            normalGoal.addGoal(1, AttackLivingGoal.builder().longMemory().speed(4).build(mannequin));
        }
        normalGoal.addGoal(2, RandomWalkingGoal.builder().speed(3).build(mannequin));
        normalGoal.addGoal(3, LookAtGoal.builder().maxDistance(8f).watch(Mannequin.class).build(mannequin));
        normalGoal.addGoal(4, LookRandomlyGoal.builder().build(mannequin));
    }

    public void initHumanEquipment(final Mannequin mannequin, final boolean archer) {
        mannequin.setLegs(ItemStack.of(ItemTypes.LEATHER_LEGGINGS));
        mannequin.setChest(ItemStack.of(ItemTypes.IRON_CHESTPLATE));
        mannequin.setFeet(ItemStack.of(ItemTypes.GOLDEN_BOOTS));
        mannequin.setItemInHand(HandTypes.MAIN_HAND, ItemStack.of(archer ? ItemTypes.BOW : ItemTypes.STONE_AXE));
        mannequin.setItemInHand(HandTypes.OFF_HAND, ItemStack.of(ItemTypes.GOLDEN_APPLE));

        mannequin.offer(Keys.MAX_HEALTH, 500d);
        mannequin.offer(Keys.HEALTH, 500d);
    }
}
