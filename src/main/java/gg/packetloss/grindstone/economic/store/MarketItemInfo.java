/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

import static gg.packetloss.grindstone.economic.store.MarketComponent.LOWER_MARKET_LOSS_THRESHOLD;

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
        return ItemNameCalculator.getUnqualifiedName(name);
    }

    public Text getDisplayName() {
        return ItemNameCalculator.getSystemDisplayName(name);
    }

    public String getDisplayNameNoColor() {
        return ItemNameCalculator.getSystemDisplayNameNoColor(name);
    }

    public String getLookupName() {
        return getUnqualifiedName().toUpperCase();
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

        ItemMeta stackMeta = stack.getItemMeta();
        if (stackMeta instanceof Damageable && ((Damageable) stackMeta).hasDamage()) {
            if (stack.getAmount() > 1) {
                return Optional.empty();
            }
            percentageSale = 1 - ((double) ((Damageable) stackMeta).getDamage() / (double) stack.getType().getMaxDurability());
        }

        return Optional.of(percentageSale);
    }

    public Optional<Double> getValueForStack(ItemStack stack) {
        Optional<Double> optPercentageSale = computePercentageSale(stack);
        if (optPercentageSale.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(optPercentageSale.get() * getValue() * stack.getAmount());
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

    public boolean hasStock() {
        return stock != -1;
    }

    public boolean hasInfiniteStock() {
        return !hasStock();
    }

    public Optional<Integer> getStock() {
        return hasStock() ? Optional.of(stock) : Optional.empty();
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
