/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemName;

public class MarketItemLookupInstance {
    private final Map<String, MarketItemInfo> priceMapping;

    public MarketItemLookupInstance(Map<String, MarketItemInfo> priceMapping) {
        this.priceMapping = priceMapping;
    }

    public Optional<MarketItem> getItemDetails(String itemName) {
        return Optional.ofNullable(priceMapping.get(itemName.toUpperCase())).map(MarketItem::new);
    }

    public Optional<MarketItem> getItemDetails(ItemStack stack) {
        return computeItemName(stack).flatMap(this::getItemDetails);
    }

    /**
     * @param stack the item to check
     * @return the value if the item were in perfect condition at an unadjusted rate
     */
    public Optional<Double> checkMaximumValue(ItemStack stack) {
        return getItemDetails(stack).map(MarketItem::getValue).map((value) -> value * stack.getAmount());
    }

    /**
     * @param stack the item to check
     * @return the value of the item in its current condition at an unadjusted rate
     */
    public Optional<Double> checkCurrentValue(ItemStack stack) {
        return getItemDetails(stack).flatMap((item) -> item.getSellPriceForStack(stack));
    }

    private Optional<MarketItem> getItemDetailsIfSellable(MarketItem marketItem) {
        if (!marketItem.isSellable()) {
            return Optional.empty();
        }

        return Optional.of(marketItem);
    }

    private Optional<MarketItem> getItemDetailsIfSellable(String itemName) {
        return getItemDetails(itemName).flatMap(this::getItemDetailsIfSellable);
    }

    private Optional<MarketItem> getItemDetailsIfSellable(ItemStack stack) {
        return getItemDetails(stack).flatMap(this::getItemDetailsIfSellable);
    }

    /**
     * @param stack the item to check
     * @return the value of the item if it were to be sold on the market as is
     */
    public Optional<Double> checkSellPrice(ItemStack stack) {
        return getItemDetailsIfSellable(stack).flatMap((item) -> item.getSellPriceForStack(stack));
    }

    /**
     * @param itemName the item to check
     * @return the value of the item if it were to be sold on the market as is
     */
    public Optional<Double> checkSellPrice(String itemName) {
        return getItemDetailsIfSellable(itemName).map(MarketItem::getSellPrice);
    }
}
