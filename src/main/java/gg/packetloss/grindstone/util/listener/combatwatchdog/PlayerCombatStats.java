/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.listener.combatwatchdog;

class PlayerCombatStats {
    private int offensiveCombatEvents;
    private int defenseCombatEvents;
    private long lastCombatStatsReset;

    public PlayerCombatStats() {
        resetCombatStats();
    }

    public void markOffensiveCombat() {
        ++offensiveCombatEvents;
    }

    public void markDefensiveCombat() {
        ++defenseCombatEvents;
    }

    public long getCombatFactors() {
        return offensiveCombatEvents + defenseCombatEvents;
    }

    public double getCombatRatio() {
        return ((double) offensiveCombatEvents) / defenseCombatEvents;
    }

    public void resetCombatStats() {
        offensiveCombatEvents = 1;
        defenseCombatEvents = 1;
        lastCombatStatsReset = System.currentTimeMillis();
    }

    public long getLastCombatStatsReset() {
        return lastCombatStatsReset;
    }
}
