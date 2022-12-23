/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import gg.packetloss.grindstone.highscore.scoretype.ScoreType.Order;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType.UpdateMethod;
import gg.packetloss.grindstone.util.BossKind;

public class ScoreTypes {
    // public static final ScoreType WILDERNESS_ORES_MINED = new ScoreType(0, true, ScoreType.Order.DESC);
    // public static final ScoreType WILDERNESS_MOB_KILLS = new ScoreType(1, true, ScoreType.Order.DESC);
    // public static final ScoreType WILDERNESS_DEATHS = new ScoreType(2, true, ScoreType.Order.DESC);
    public static final ScoreType JUNGLE_RAID_WINS = new BasicScoreType(3, false, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType GOLD_RUSH_ROBBERIES = new BasicScoreType(4, true, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType FASTEST_GOLD_RUSH = new TimeBasedScoreType(5, true, UpdateMethod.OVERRIDE_IF_BETTER, Order.ASC);
    public static final ScaledScoreType SACRIFICED_VALUE = new ScaledScoreType(6, true, UpdateMethod.INCREMENTAL, Order.DESC, 60.243);
    public static final ScoreType COW_KILLS = new KillCounterScoreType(7, true);
    public static final ScoreType APOCALYPSE_MOBS_SLAIN = new KillCounterScoreType(8, true);
    public static final ScoreType CURSED_ORES_MINED = new BasicScoreType(9, true, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType CURSED_MINE_DEATHS = new BasicScoreType(10, true, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType MIRAGE_ARENA_KILLS = new KillCounterScoreType(11, false);
    public static final ScoreType SKY_WARS_WINS = new BasicScoreType(12, false, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType SHNUGGLES_PRIME_SOLO_KILLS = new BossKillCounterScoreType(13, true, BossKind.SHNUGGLES_PRIME);
    public static final ScoreType SHNUGGLES_PRIME_TEAM_KILLS = new BossKillCounterScoreType(14, true, BossKind.SHNUGGLES_PRIME);
    public static final ScoreType NINJA_LEVEL = new ExpBasedScoreType(15);
    public static final ScoreType ROGUE_LEVEL = new ExpBasedScoreType(16);
    public static final ScoreType NINJA_PARKOUR_FASTEST_RUN = new TimeBasedScoreType(17, false, UpdateMethod.OVERRIDE_IF_BETTER, Order.ASC);
    public static final ScoreType NINJA_PARKOUR_CROSSINGS = new BasicScoreType(18, false, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType GRAVE_YARD_LOOTINGS = new BasicScoreType(19, true, UpdateMethod.INCREMENTAL, Order.DESC);
    public static final ScoreType FREAKY_FOUR_KILLS = new BossKillCounterScoreType(20, true, BossKind.FREAKY_FOUR);
    public static final ScoreType PATIENT_X_SOLO_KILLS = new BossKillCounterScoreType(21, true, BossKind.PATIENT_X);
    public static final ScoreType PATIENT_X_TEAM_KILLS = new BossKillCounterScoreType(22, true, BossKind.PATIENT_X);
    public static final ScoreType FROSTBORN_SOLO_KILLS = new BossKillCounterScoreType(23, true, BossKind.FROSTBORN);
    public static final ScoreType FROSTBORN_TEAM_KILLS = new BossKillCounterScoreType(24, true, BossKind.FROSTBORN);
    public static final ScoreType BESSI_SOLO_KILLS = new BossKillCounterScoreType(25, true, BossKind.BESSI);
    public static final ScoreType BESSI_TEAM_KILLS = new BossKillCounterScoreType(26, true, BossKind.BESSI);
    public static final ScoreType SKRIN = new CurrencyScoreType(25);


    // public static final ScoreType GOBLET_XXX = new ProxyScoreType(1XX'XXX);
}
