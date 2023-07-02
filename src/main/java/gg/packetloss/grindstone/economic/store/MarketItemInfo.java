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
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static gg.packetloss.grindstone.economic.store.MarketComponent.LOWER_MARKET_LOSS_THRESHOLD;

public class MarketItemInfo implements Comparable<MarketItemInfo> {
    private String name;
    private BigDecimal value;
    private BigDecimal price;
    private int stock;
    private boolean infinite;
    private boolean disableBuy;
    private boolean disableSell;

    public MarketItemInfo(String name, BigDecimal value, BigDecimal price, int stock, boolean infinite,
                          boolean disableBuy, boolean disableSell) {
        this.name = name;
        this.value = value;
        this.price = price;
        this.stock = stock;
        this.infinite = infinite;
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

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }

    private static BigDecimal doRounding(BigDecimal input) {
        return input.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPrice() {
        return doRounding(price);
    }

    /*
    The tax applied when an item equals or exceeds LOWER_MARKET_LOSS_THRESHOLD.
     */
    private static final BigDecimal EXPENSIVE_ITEM_TAX = BigDecimal.valueOf(.92);
    /*
    The tax applied when an item is below the LOWER_MARKET_LOSS_THRESHOLD.
     */
    private static final BigDecimal CHEAP_ITEM_TAX = BigDecimal.valueOf(.80);

    private BigDecimal getSellPriceNoRounding() {
        if (price.compareTo(LOWER_MARKET_LOSS_THRESHOLD) >= 0) {
            return price.multiply(EXPENSIVE_ITEM_TAX);
        } else {
            return price.multiply(CHEAP_ITEM_TAX);
        }
    }

    public BigDecimal getSellPrice() {
        return doRounding(getSellPriceNoRounding());
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

    public Optional<BigDecimal> getValueForStack(ItemStack stack) {
        Optional<Double> optPercentageSale = computePercentageSale(stack);
        if (optPercentageSale.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal value = getValue();
        BigDecimal percentSale = BigDecimal.valueOf(optPercentageSale.get());
        BigDecimal amount = BigDecimal.valueOf(stack.getAmount());
        return Optional.of(value.multiply(percentSale).multiply(amount));
    }

    public Optional<BigDecimal> getSellUnitPriceForStack(ItemStack stack) {
        Optional<Double> optPercentageSale = computePercentageSale(stack);
        if (optPercentageSale.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal salePrice = getSellPrice();
        BigDecimal percentSale = BigDecimal.valueOf(optPercentageSale.get());
        return Optional.of(salePrice.multiply(percentSale));
    }

    public Optional<BigDecimal> getSellPriceForStack(ItemStack stack) {
        BigDecimal amount = BigDecimal.valueOf(stack.getAmount());
        return getSellUnitPriceForStack(stack).map((value) -> value.multiply(amount));
    }

    public boolean hasInfiniteStock() {
        return infinite;
    }

    public Optional<Integer> getStock() {
        return hasInfiniteStock() ? Optional.empty() : Optional.of(stock);
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
    public int compareTo(@NotNull MarketItemInfo record) {
        /* Sort by price, then fallback to name. */
        int priceComparison = this.getPrice().compareTo(record.getPrice());
        if (priceComparison != 0) {
            return priceComparison;
        } else {
            return this.getUnqualifiedName().compareTo(record.getUnqualifiedName());
        }
    }
}
