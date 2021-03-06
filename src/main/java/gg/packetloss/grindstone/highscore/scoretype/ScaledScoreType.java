/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import java.math.BigDecimal;

public class ScaledScoreType extends BasicScoreType {
    private final double scalingConstant;

    protected ScaledScoreType(int id, boolean requirements, boolean incremental, Order order, double scalingConstant) {
        super(id, requirements, incremental, order);
        this.scalingConstant = scalingConstant;
    }

    public double getScalingConstant() {
        return scalingConstant;
    }

    public String format(long score) {
        BigDecimal decimal = new BigDecimal(score);
        decimal = decimal.multiply(new BigDecimal(scalingConstant));
        return format(decimal.toBigInteger());
    }
}
