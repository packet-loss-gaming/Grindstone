/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

import static gg.packetloss.grindstone.util.StringUtil.toTitleCase;

public class MarketItem implements Comparable<MarketItem> {
    private MarketItemInfo itemInfo;

    public MarketItem(MarketItemInfo itemInfo) {
        this.itemInfo = itemInfo;
    }

    public String getName() {
        return itemInfo.getName();
    }

    public String getTitleCasedName() {
        return toTitleCase(itemInfo.getUnqualifiedName());
    }

    public String getDisplayName() {
        return itemInfo.getDisplayName();
    }

    public String getLookupName() {
        return itemInfo.getLookupName();
    }

    public double getValue() {
        return itemInfo.getValue();
    }

    public Optional<Double> getValueForStack(ItemStack stack) {
        return itemInfo.getValueForStack(stack);
    }

    public double getPrice() {
        return itemInfo.getPrice();
    }

    public double getSellPrice() {
        return itemInfo.getSellPrice();
    }

    public Optional<Double> getSellUnitPriceForStack(ItemStack stack) {
        return itemInfo.getSellUnitPriceForStack(stack);
    }

    public Optional<Double> getSellPriceForStack(ItemStack stack) {
        return itemInfo.getSellPriceForStack(stack);
    }

    public int getStock() {
        return itemInfo.getStock();
    }

    public boolean isEnabled() {
        return itemInfo.isEnabled();
    }

    public boolean isDisabled() {
        return itemInfo.isDisabled();
    }

    public boolean isBuyable() {
        return itemInfo.isBuyable();
    }

    public boolean isSellable() {
        return itemInfo.isSellable();
    }

    public boolean displayBuyInfo() {
        return itemInfo.displayBuyInfo();
    }

    public boolean displaySellInfo() {
        return itemInfo.displaySellInfo();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MarketItem && itemInfo.getName().equals(((MarketItem) o).getName());
    }

    @Override
    public int hashCode() {
        return itemInfo.getName().hashCode();
    }

    @Override
    public int compareTo(MarketItem o) {
        return itemInfo.compareTo(o.itemInfo);
    }
}
