/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

import static gg.packetloss.grindstone.economic.store.MarketComponent.LOWER_MARKET_LOSS_THRESHOLD;
import static gg.packetloss.grindstone.util.StringUtil.toUppercaseTitle;

public class MarketItemInfo implements Comparable<MarketItemInfo> {
    private String name;
    private double value, price;
    private int stock;
    private boolean disableBuy, disableSell;

    public MarketItemInfo(String name, double value, double price, int stock, boolean disableBuy, boolean disableSell) {
        this.name = name;
        this.value = value;
        this.price = price;
        this.stock = stock;
        this.disableBuy = disableBuy;
        this.disableSell = disableSell;
    }

    public String getName() {
        return name;
    }

    public String getUnqualifiedName() {
        return name.split(":")[1];
    }

    public String getDisplayName() {
        return toUppercaseTitle(getUnqualifiedName());
    }

    public String getLookupName() {
        return getDisplayName();
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    private double rounded(double input) {
        double scale = Math.pow(10, 2);
        return Math.round(input * scale) / scale;
    }

    public double getPrice() {
        return rounded(price);
    }

    public double getSellPrice() {
        double sellPrice = price >= LOWER_MARKET_LOSS_THRESHOLD ? price * .92 : price * .80;
        return rounded(sellPrice);
    }

    private Optional<Double> computePercentageSale(ItemStack stack) {
        double percentageSale = 1;
        if (stack.getDurability() != 0 && stack.getType().getMaxDurability() != 0) {
            if (stack.getAmount() > 1) {
                return Optional.empty();
            }
            percentageSale = 1 - ((double) stack.getDurability() / (double) stack.getType().getMaxDurability());
        }
        return Optional.of(percentageSale);
    }

    public Optional<Double> getSellUnitPriceForStack(ItemStack stack) {
        Optional<Double> optPercentageSale = computePercentageSale(stack);
        if (optPercentageSale.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(optPercentageSale.get() * getSellPrice());
    }

    public Optional<Double> getSellPriceForStack(ItemStack stack) {
        return getSellUnitPriceForStack(stack).map((value) -> value * stack.getAmount());
    }

    public int getStock() {
        return stock;
    }

    public boolean isEnabled() {
        return isBuyable() || isSellable();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public boolean isBuyable() {
        return !disableBuy;
    }

    public boolean isSellable() {
        return !disableSell;
    }

    public boolean displayBuyInfo() {
        return isBuyable() || isDisabled();
    }

    public boolean displaySellInfo() {
        return isSellable() || isDisabled();
    }

    @Override
    public int compareTo(MarketItemInfo record) {
        if (record == null) return -1;
        if (this.getPrice() == record.getPrice()) {
            int c = this.getUnqualifiedName().compareTo(record.getUnqualifiedName());
            return c == 0 ? 1 : c;
        }
        return this.getPrice() > record.getPrice() ? 1 : -1;
    }
}
