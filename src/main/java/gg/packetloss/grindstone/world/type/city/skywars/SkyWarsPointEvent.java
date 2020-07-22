/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.skywars;

public enum SkyWarsPointEvent {
    GAME_WON(100, "VICTORY!");

    private final int amount;
    private final String caption;

    private SkyWarsPointEvent(int amount, String caption) {
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
