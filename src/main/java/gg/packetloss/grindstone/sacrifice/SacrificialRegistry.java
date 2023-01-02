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
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class SacrificialRegistry {
    private final Set<String> registeredStacks = new HashSet<>();
    private final List<Supplier<ItemStack>> junk = new ArrayList<>();
    private final List<ChancedEntry> valuable = new ArrayList<>();

    private long lastLookupUpdate;
    private MarketItemLookupInstance lookupInstance;

    public void registerItem(Supplier<ItemStack> dropSupplier, SacrificeCommonality commonality) {
        ItemNameCalculator.computeItemName(dropSupplier.get()).ifPresent(registeredStacks::add);

        if (commonality == SacrificeCommonality.JUNK) {
            junk.add(dropSupplier);
            return;
        }

        if (!valuable.isEmpty()) {
            SacrificeCommonality precedingCommonality = valuable.get(valuable.size() - 1).commonality;
            Validate.isTrue(precedingCommonality.getAdditionalChance() <= commonality.getAdditionalChance());
        }

        valuable.add(new ChancedEntry(dropSupplier, commonality));
    }

    private boolean getChance(SacrificeInformation sacrificeInformation, SacrificeCommonality commonality) {
        int baseChance = commonality.getRoll(sacrificeInformation.getCommonalityModifier());
        int chanceModifier = sacrificeInformation.getModifierRoll();

        return ChanceUtil.getChance(Math.max(1, baseChance - chanceModifier));
    }

    private ItemStack getValuableItem(SacrificeInformation sacrificeInformation) {
        int position = valuable.size();
        do {
            // Update the position with a strong biased towards higher value items
            position = Math.max(0, position - ChanceUtil.getRandomNTimes(valuable.size(), 3));

            ChancedEntry supplier = valuable.get(position);
            if (supplier.commonality == SacrificeCommonality.NORMAL) {
                return supplier.dropSupplier.get();
            }

            if (getChance(sacrificeInformation, supplier.commonality)) {
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

    protected double getValue(MarketItemLookupInstance lookupInstance, ItemStack itemStack) {
        // FIXME: These can be added back to the market now
        if (SpawnEgg.fromMaterial(itemStack.getType()) != null) {
            return 12.5 * itemStack.getAmount();
        }
        return lookupInstance.checkMaximumValue(itemStack).orElse(0d);
    }

    public double getValue(ItemStack itemStack) {
        return getValue(MarketComponent.getLookupInstanceFromStacksImmediately(List.of(itemStack)), itemStack);
    }

    private static final int MINIMUM_REMOVAL_VALUE = 9;

    private int getValueChance(SacrificeInformation sacrificeInformation) {
        int baseChance = (sacrificeInformation.hasSacrificeTome() ? 100 : 125);
        baseChance -= sacrificeInformation.getModifierRoll();
        return Math.max(1, baseChance);
    }

    public SacrificeResult getCalculatedLoot(SacrificeInformation sacrificeInformation) {
        List<ItemStack> loot = new ArrayList<>();

        int valuableChance = getValueChance(sacrificeInformation);

        int remainingItems = sacrificeInformation.getMaxItems();
        double remainingValue = sacrificeInformation.getValue();

        MarketItemLookupInstance lookupInstance = getLookupInstance();

        while (remainingValue > 0 && (remainingItems == -1 || remainingItems > 0)) {
            ItemStack itemStack;

            if (ChanceUtil.getChance(valuableChance)) {
                itemStack = getValuableItem(sacrificeInformation);
            } else if (sacrificeInformation.hasCleanlyTome()) {
                remainingValue -= MINIMUM_REMOVAL_VALUE;
                continue;
            } else {
                itemStack = getJunkStack();
            }

            if (itemStack != null) {
                remainingValue -= Math.max(MINIMUM_REMOVAL_VALUE, getValue(lookupInstance, itemStack));
                loot.add(itemStack);
            }

            if (remainingItems != -1) {
                remainingItems--;
            }
        }

        return new SacrificeResult(loot, remainingValue, remainingItems);
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
