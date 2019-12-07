package gg.packetloss.grindstone.util.region;

public class RegionValueReport {
    private double blockPrice;
    private double itemPriceCurrentState;
    private double maximumItemValue;

    public RegionValueReport(double blockPrice, double itemPriceCurrentState, double maximumItemValue) {
        this.blockPrice = blockPrice;
        this.itemPriceCurrentState = itemPriceCurrentState;
        this.maximumItemValue = maximumItemValue;
    }

    public double getBlockPrice() {
        return blockPrice;
    }

    public double getItemPriceCurrentState() {
        return itemPriceCurrentState;
    }

    public double getMaximumItemValue() {
        return maximumItemValue;
    }

    public double getAutoSellPrice() {
        return blockPrice + itemPriceCurrentState;
    }
}
