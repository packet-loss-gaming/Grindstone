/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.region;

import java.math.BigDecimal;

public class RegionValueReport {
    private BigDecimal blockPrice;
    private BigDecimal itemPriceCurrentState;
    private BigDecimal maximumItemValue;

    public RegionValueReport(BigDecimal blockPrice, BigDecimal itemPriceCurrentState, BigDecimal maximumItemValue) {
        this.blockPrice = blockPrice;
        this.itemPriceCurrentState = itemPriceCurrentState;
        this.maximumItemValue = maximumItemValue;
    }

    public BigDecimal getBlockPrice() {
        return blockPrice;
    }

    public BigDecimal getItemPriceCurrentState() {
        return itemPriceCurrentState;
    }

    public BigDecimal getMaximumItemValue() {
        return maximumItemValue;
    }

    public BigDecimal getAutoSellPrice() {
        return blockPrice.add(itemPriceCurrentState);
    }
}
