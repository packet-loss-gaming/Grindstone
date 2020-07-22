/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
