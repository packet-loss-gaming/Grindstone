package gg.packetloss.grindstone.economic.store;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class MarketItem {
    private MarketItemInfo itemInfo;

    public MarketItem(MarketItemInfo itemInfo) {
        this.itemInfo = itemInfo;
    }

    public String getName() {
        return itemInfo.getName();
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

}
