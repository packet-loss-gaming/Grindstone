/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.powers;

public enum RoguePower implements GuildPower {
    BERSERKER(3),
    PITFALL_LEAP(5),
    FALL_DAMAGE_REDIRECTION(6),
    POTION_METABOLIZATION(7),
    NIGHTMARE_SPECIAL(12),
    SNIPER_SNOWBALLS(17),
    BACKSTAB(20),
    LIKE_A_METEOR(23),
    SUPER_SPEED(30);

    private final int level;

    private RoguePower(int level) {
        this.level = level;
    }

    @Override
    public int getUnlockLevel() {
        return level;
    }
}
