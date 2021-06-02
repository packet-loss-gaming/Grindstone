/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import gg.packetloss.grindstone.highscore.scoretype.GobletScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GobletState {
    private static final int GOBLET_SCORE_ID = 100000;

    private int scoreTypeId = -1;
    private int monthOfYear = 0;
    private int year = 0;

    private HashMap<UUID, Integer> winners = new HashMap<>();

    public boolean isStale() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.MONTH) != monthOfYear || cal.get(Calendar.YEAR) != year;
    }

    public boolean wasActive() {
        return scoreTypeId != -1;
    }

    public void setScoreType(ScoreType scoreType) {
        scoreTypeId = scoreType.getId();

        Calendar cal = Calendar.getInstance();
        monthOfYear = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);
    }

    public GobletScoreType loadScoreType(Map<Integer, ScoreType> idToScoreType) {
        return new GobletScoreType(GOBLET_SCORE_ID, idToScoreType.get(scoreTypeId));
    }

    public void addWinner(UUID playerID) {
        winners.merge(playerID, 1, Integer::sum);
    }

    public int checkWinner(UUID playerID) {
        return winners.remove(playerID);
    }
}
