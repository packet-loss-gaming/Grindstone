/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.jungleraid;

public enum  JungleRaidPointEvent {
    PLAYER_KILL(25, "Player Killed!"),
    LONG_SHOT(25, "Long Shot!"),
    EPIC_LONG_SHOT(50, "EPIC Long Shot!"),
    TITAN_KILLED(75, "TITAN KILLED!"),
    GAME_WON(100, "VICTORY!");

    private final int amount;
    private final String caption;

    private JungleRaidPointEvent(int amount, String caption) {
        this.amount = amount;
        this.caption = caption;
    }

    public int getAdjustment() {
        return amount;
    }

    public String getCaption() {
        return caption;
    }
}
