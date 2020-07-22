/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

import java.util.Collection;

public class MassBossKillInfo implements KillInfo {
    private final Collection<Player> players;

    public MassBossKillInfo(Collection<Player> players) {
        this.players = players;
    }

    public Collection<Player> getPlayers() {
        return players;
    }
}
