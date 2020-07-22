/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

public class MassBossPlayerKillInfo {
    private final MassBossKillInfo info;
    private final Player player;

    public MassBossPlayerKillInfo(MassBossKillInfo info, Player player) {
        this.info = info;
        this.player = player;
    }

    public MassBossKillInfo getKillInfo() {
        return info;
    }

    public Player getPlayer() {
        return player;
    }
}
