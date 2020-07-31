/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class ScoreType {
    private final DecimalFormat DEFAULT_FORMATTER = new DecimalFormat("#,###");

    private final int id;
    private final boolean incremental;
    private final Order order;

    protected ScoreType(int id, boolean incremental, Order order) {
        this.id = id;
        this.incremental = incremental;
        this.order = order;
    }

    public int getId() {
        return id;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public Order getOrder() {
        return order;
    }

    protected final String format(BigInteger integer) {
        return DEFAULT_FORMATTER.format(integer);
    }

    public String format(long score) {
        return format(BigInteger.valueOf(score));
    }

    public enum Order {
        ASC, DESC
    }
}
