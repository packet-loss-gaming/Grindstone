/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import java.math.BigDecimal;

public class ScaledScoreType extends BasicScoreType {
    private final BigDecimal scalingConstant;

    protected ScaledScoreType(int id, boolean requirements, UpdateMethod updateMethod, Order order, double scalingConstant) {
        super(id, requirements, updateMethod, order);
        this.scalingConstant = BigDecimal.valueOf(scalingConstant);
    }

    public BigDecimal getScalingConstant() {
        return scalingConstant;
    }

    public String format(long score) {
        BigDecimal decimal = new BigDecimal(score);
        decimal = decimal.multiply(scalingConstant);
        return format(decimal.toBigInteger());
    }
}
