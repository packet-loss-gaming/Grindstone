/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import gg.packetloss.grindstone.highscore.scoretype.ScoreType;

import java.math.BigInteger;
import java.util.UUID;

public class HighScoreUpdate {
    private final UUID playerId;
    private final ScoreType scoreType;
    private final BigInteger value;

    public HighScoreUpdate(UUID playerId, ScoreType scoreType, BigInteger value) {
        this.playerId = playerId;
        this.scoreType = scoreType;
        this.value = value;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ScoreType getScoreType() {
        return scoreType;
    }

    public BigInteger getValue() {
        return value;
    }
}
