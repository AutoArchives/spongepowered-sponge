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
package org.spongepowered.common.mixin.api.minecraft.world.item.trading;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.ProfessionTypes;
import org.spongepowered.api.item.merchant.TradeOffer;
import org.spongepowered.api.item.merchant.TradeOfferGenerator;
import org.spongepowered.api.util.RandomProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.VecHelper;

import java.util.Optional;

@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin_API implements TradeOfferGenerator {

    // @formatter:off
    @Shadow public abstract MerchantOffer shadow$getOffer(final LootContext lootContext);
    // @formatter:on

    @Override
    public TradeOffer apply(final org.spongepowered.api.entity.Entity merchant, final RandomProvider.Source random) {
        if (!(merchant.world() instanceof ServerLevel sl)) {
            throw new IllegalStateException("The merchant is not in a server world!");
        }
        final var profession = merchant.get(Keys.PROFESSION_TYPE).orElseGet(ProfessionTypes.NITWIT);
        final var level = merchant.get(Keys.PROFESSION_LEVEL).orElse(1);
        final var trades = ((VillagerProfession) (Object) profession).getTrades(level);

        Optional<TradeSet> tradeSetOpt = sl.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(trades);
        if (tradeSetOpt.isEmpty()) {
            throw new IllegalStateException("The merchant is not in a server world!");
        }

        LootContext lootContext = new LootContext.Builder(
            new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, VecHelper.toVanillaVector3d(merchant.position()))
                .withParameter(LootContextParams.THIS_ENTITY, (Entity) merchant)
                .create(LootContextParamSets.VILLAGER_TRADE)
        )
            .create(tradeSetOpt.get().randomSequence());
        return (TradeOffer) this.shadow$getOffer(lootContext);
    }

}
