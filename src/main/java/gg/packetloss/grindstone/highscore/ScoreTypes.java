/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

public class ScoreTypes {
    public static final ScoreType WILDERNESS_ORES_MINED = new ScoreType(0, true, ScoreType.Order.DESC);
    public static final ScoreType WILDERNESS_MOB_KILLS = new ScoreType(1, true, ScoreType.Order.DESC);
    public static final ScoreType WILDERNESS_DEATHS = new ScoreType(2, true, ScoreType.Order.DESC);
    public static final ScoreType JUNGLE_RAID_WINS = new ScoreType(3, true, ScoreType.Order.DESC);
    public static final ScoreType GOLD_RUSH_ROBBERIES = new ScoreType(4, true, ScoreType.Order.DESC);
    public static final ScoreType FASTEST_GOLD_RUSH = new TimeBasedScoreType(5, false, ScoreType.Order.ASC);
}