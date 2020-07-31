/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

public class ScoreTypes {
    // public static final ScoreType WILDERNESS_ORES_MINED = new ScoreType(0, true, ScoreType.Order.DESC);
    // public static final ScoreType WILDERNESS_MOB_KILLS = new ScoreType(1, true, ScoreType.Order.DESC);
    // public static final ScoreType WILDERNESS_DEATHS = new ScoreType(2, true, ScoreType.Order.DESC);
    public static final ScoreType JUNGLE_RAID_WINS = new ScoreType(3, true, ScoreType.Order.DESC);
    public static final ScoreType GOLD_RUSH_ROBBERIES = new ScoreType(4, true, ScoreType.Order.DESC);
    public static final ScoreType FASTEST_GOLD_RUSH = new TimeBasedScoreType(5, false, ScoreType.Order.ASC);
    public static final ScaledScoreType SACRIFICED_VALUE = new ScaledScoreType(6, true, ScoreType.Order.DESC, 60.243);
    public static final ScoreType COW_KILLS = new ScoreType(7, true, ScoreType.Order.DESC);
    public static final ScoreType APOCALYPSE_MOBS_SLAIN = new ScoreType(8, true, ScoreType.Order.DESC);
    public static final ScoreType CURSED_ORES_MINED = new ScoreType(9, true, ScoreType.Order.DESC);
    public static final ScoreType CURSED_MINE_DEATHS = new ScoreType(10, true, ScoreType.Order.DESC);
    public static final ScoreType MIRAGE_ARENA_KILLS = new ScoreType(11, true, ScoreType.Order.DESC);
    public static final ScoreType SKY_WARS_WINS = new ScoreType(12, true, ScoreType.Order.DESC);
    public static final ScoreType SHNUGGLES_PRIME_SOLO_KILLS = new ScoreType(13, true, ScoreType.Order.DESC);
    public static final ScoreType SHNUGGLES_PRIME_TEAM_KILLS = new ScoreType(14, true, ScoreType.Order.DESC);
    public static final ScoreType NINJA_LEVEL = new ExpBasedScoreType(15);
    public static final ScoreType ROGUE_LEVEL = new ExpBasedScoreType(16);
    public static final ScoreType NINJA_PARKOUR_FASTEST_RUN = new TimeBasedScoreType(17, false, ScoreType.Order.ASC);
    public static final ScoreType NINJA_PARKOUR_CROSSINGS = new ScoreType(18, true, ScoreType.Order.DESC);
    public static final ScoreType GRAVE_YARD_LOOTINGS = new ScoreType(19, true, ScoreType.Order.DESC);
    public static final ScoreType FREAKY_FOUR_KILLS = new ScoreType(20, true, ScoreType.Order.DESC);
    public static final ScoreType PATIENT_X_SOLO_KILLS = new ScoreType(21, true, ScoreType.Order.DESC);
    public static final ScoreType PATIENT_X_TEAM_KILLS = new ScoreType(22, true, ScoreType.Order.DESC);
    public static final ScoreType FROSTBORN_SOLO_KILLS = new ScoreType(23, true, ScoreType.Order.DESC);
    public static final ScoreType FROSTBORN_TEAM_KILLS = new ScoreType(24, true, ScoreType.Order.DESC);
}
