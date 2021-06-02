/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.NinjaParkour;

import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NinjaParkourListener extends AreaListener<NinjaParkour> {
    public NinjaParkourListener(NinjaParkour parent) {
        super(parent);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            parent.teleportToStart(player);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();

        if (parent.contains(to) && !event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)) {
            event.setTo(parent.resetPoint.clone());
        }
    }
}
