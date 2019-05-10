/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

public class ItemPricePair implements Comparable<ItemPricePair> {

    private String name;
    private double price;
    private boolean disableBuy, disableSell;

    public ItemPricePair(String name, double price, boolean disableBuy, boolean disableSell) {
        this.name = name;
        this.price = price;
        this.disableBuy = disableBuy;
        this.disableSell = disableSell;
    }

    public String getName() {
        return name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public double getSellPrice() {
        double sellPrice = price > 100000 ? price * .92 : price * .80;
        return sellPrice < .01 ? 0 : sellPrice;
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
    public int compareTo(ItemPricePair record) {
        if (record == null) return -1;
        if (this.getPrice() == record.getPrice()) {
            int c = String.CASE_INSENSITIVE_ORDER.compare(this.getName(), record.getName());
            return c == 0 ? 1 : c;
        }
        return this.getPrice() > record.getPrice() ? 1 : -1;
    }
}
