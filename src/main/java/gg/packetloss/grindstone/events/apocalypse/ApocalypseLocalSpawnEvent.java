/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ApocalypseLocalSpawnEvent extends ApocalypsePlayerEvent {
    public ApocalypseLocalSpawnEvent(Player player, Location spawnLocation) {
        super(player, spawnLocation);
    }
}
