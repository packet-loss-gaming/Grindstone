/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.SpawnEgg;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class SacrificialRegistry {
    private Set<String> registeredStacks = new HashSet<>();
    private List<Supplier<ItemStack>> junk = new ArrayList<>();
    private List<ChancedEntry> valuable = new ArrayList<>();

    private long lastLookupUpdate;
    private MarketItemLookupInstance lookupInstance;

    public void registerItem(Supplier<ItemStack> dropSupplier, SacrificeCommonality commonality) {
        ItemNameCalculator.computeItemName(dropSupplier.get()).ifPresent(registeredStacks::add);

        if (commonality == SacrificeCommonality.JUNK) {
            junk.add(dropSupplier);
            return;
        }

        valuable.add(new ChancedEntry(dropSupplier, commonality));
    }

    private int calculateModifier(double value) {
        return (int) (Math.sqrt(value) * 1.5);
    }

    private boolean getChance(CommandSender sender, int modifier, double rarityL) {
        boolean hasEfficiency = sender.hasPermission("aurora.sacrifice.efficiency");
        int baseChance = (int) (hasEfficiency ? rarityL * 100 : rarityL * 200);

        return ChanceUtil.getChance(Math.max(1, baseChance - modifier));
    }

    private ItemStack getValuableItem(CommandSender sender, int modifier) {
        do {
            ChancedEntry supplier = CollectionUtil.getElement(valuable);
            if (supplier.commonality == SacrificeCommonality.NORMAL) {
                return supplier.dropSupplier.get();
            }

            if (getChance(sender, modifier, supplier.commonality.getAdditionalChance())) {
                return supplier.dropSupplier.get();
            }
        } while (true);
    }

    private ItemStack getJunkStack() {
        return CollectionUtil.getElement(junk).get();
    }

    private MarketItemLookupInstance getLookupInstance() {
        if (lookupInstance == null || System.currentTimeMillis() - lastLookupUpdate >= TimeUnit.HOURS.toMillis(1)) {
            lastLookupUpdate = System.currentTimeMillis();
            lookupInstance = MarketComponent.getLookupInstance(registeredStacks);
        }

        return lookupInstance;
    }

    private double getValue(MarketItemLookupInstance lookupInstance, ItemStack itemStack) {
        // FIXME: These can be added back to the market now
        if (SpawnEgg.fromMaterial(itemStack.getType()) != null) {
            return 12.5;
        }
        return lookupInstance.checkCurrentValue(itemStack).orElse(0d);
    }

    public double getValue(ItemStack itemStack) {
        return getValue(MarketComponent.getLookupInstanceFromStacksImmediately(List.of(itemStack)), itemStack);
    }

    private static final int MINIMUM_REMOVAL_VALUE = 9;

    public List<ItemStack> getCalculatedLoot(CommandSender sender, int max, double value) {
        List<ItemStack> loot = new ArrayList<>();

        // Calculate the modifier
        int baseChance = sender.hasPermission("aurora.tome.sacrifice") ? 100 : 125;
        int modifier = calculateModifier(value);

        value *= .9;

        MarketItemLookupInstance lookupInstance = getLookupInstance();

        while (value > 0 && (max == -1 || max > 0)) {
            ItemStack itemStack;

            if (ChanceUtil.getChance(Math.max(1, baseChance - modifier))) {
                itemStack = getValuableItem(sender, modifier);
            } else if (sender.hasPermission("aurora.tome.cleanly")) {
                value -= MINIMUM_REMOVAL_VALUE;
                continue;
            } else {
                itemStack = getJunkStack();
            }

            if (itemStack != null) {
                value -= Math.max(MINIMUM_REMOVAL_VALUE, getValue(lookupInstance, itemStack));
                loot.add(itemStack);
            }

            if (max != -1) {
                max--;
            }
        }
        return loot;
    }

    private static class ChancedEntry {
        final Supplier<ItemStack> dropSupplier;
        final SacrificeCommonality commonality;

        public ChancedEntry(Supplier<ItemStack> dropSupplier, SacrificeCommonality commonality) {
            this.dropSupplier = dropSupplier;
            this.commonality = commonality;
        }
    }
}
