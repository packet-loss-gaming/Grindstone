/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

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

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public double getPrice() {
        return price;
    }

    public double getSellPrice() {
        double sellPrice = price >= LOWER_MARKET_LOSS_THRESHOLD ? price * .92 : price * .80;
        return sellPrice < .01 ? 0 : sellPrice;
    }

    public int getStock() {
        return stock;
    }

    public boolean isEnabled() {
        return isBuyable() || isSellable();
    }

    public boolean isBuyable() {
        return !disableBuy;
    }

    public boolean isSellable() {
        return !disableSell;
    }

    @Override
    public int compareTo(MarketItemInfo record) {
        if (record == null) return -1;
        if (this.getPrice() == record.getPrice()) {
            int c = String.CASE_INSENSITIVE_ORDER.compare(this.getName(), record.getName());
            return c == 0 ? 1 : c;
        }
        return this.getPrice() > record.getPrice() ? 1 : -1;
    }
}
