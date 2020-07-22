/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player;

public class PlayerExperience {
    private final float exp;
    private final int level;

    public PlayerExperience(float exp, int level) {
        this.exp = exp;
        this.level = level;
    }

    public float getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }
}
