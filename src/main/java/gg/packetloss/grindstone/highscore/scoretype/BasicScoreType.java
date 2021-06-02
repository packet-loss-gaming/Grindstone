/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Optional;

public class BasicScoreType implements ScoreType {
    private final DecimalFormat DEFAULT_FORMATTER = new DecimalFormat("#,###");

    private final int id;
    private final boolean gobletEnabled;
    private final boolean incremental;
    private final Order order;

    protected BasicScoreType(int id, boolean gobletEnabled, boolean incremental, Order order) {
        this.id = id;
        this.gobletEnabled = gobletEnabled;
        this.incremental = incremental;
        this.order = order;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isEnabledForGoblet() {
        return gobletEnabled;
    }

    @Override
    public Optional<String> getGobletName() {
        return Optional.empty();
    }

    public boolean isGobletEquivalentImpl(ScoreType scoreType) {
        return id == scoreType.getId();
    }

    @Override
    public final boolean isGobletEquivalent(ScoreType scoreType) {
        if (scoreType instanceof GobletScoreType) {
            scoreType = ((GobletScoreType) scoreType).getBaseScoreType();
        }

        return isGobletEquivalentImpl(scoreType);
    }

    @Override
    public boolean isIncremental() {
        return incremental;
    }

    @Override
    public Order getOrder() {
        return order;
    }

    protected final String format(BigInteger integer) {
        return DEFAULT_FORMATTER.format(integer);
    }

    @Override
    public String format(long score) {
        return format(BigInteger.valueOf(score));
    }
}
