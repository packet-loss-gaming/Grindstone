/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import org.apache.commons.lang.Validate;

import java.util.Optional;

public class GobletScoreType implements ScoreType {
    private final int id;
    private final ScoreType baseScoreType;

    public GobletScoreType(int id, ScoreType baseScoreType) {
        Validate.isTrue(baseScoreType.isEnabledForGoblet());

        this.id = id;
        this.baseScoreType = baseScoreType;
    }

    @Override
    public int getId() {
        return id;
    }

    public ScoreType getBaseScoreType() {
        return baseScoreType;
    }

    @Override
    public final boolean isEnabledForGoblet() {
        return true;
    }

    @Override
    public Optional<String> getGobletName() {
        return baseScoreType.getGobletName();
    }

    @Override
    public boolean isGobletEquivalent(ScoreType scoreType) {
        return baseScoreType.isGobletEquivalent(scoreType);
    }

    @Override
    public boolean isIncremental() {
        return baseScoreType.isIncremental();
    }

    @Override
    public Order getOrder() {
        return baseScoreType.getOrder();
    }

    @Override
    public String format(long score) {
        return baseScoreType.format(score);
    }
}
