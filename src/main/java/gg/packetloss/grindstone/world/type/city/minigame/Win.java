/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.minigame;

public class Win {
    private String name;
    private WinType winType;

    public Win(String name, WinType winType) {
        this.name = name;
        this.winType = winType;
    }

    public String getName() {
        return name;
    }

    public WinType getWinType() {
        return winType;
    }
}
