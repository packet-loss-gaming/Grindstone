package gg.packetloss.grindstone.util.region;

public class RegionValueReport {
    private double blockPrice;
    private double itemPrice;

    public RegionValueReport(double blockPrice, double itemPrice) {
        this.blockPrice = blockPrice;
        this.itemPrice = itemPrice;
    }

    public double getBlockPrice() {
        return blockPrice;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public double getTotalPrice() {
        return blockPrice + itemPrice;
    }
}
