/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.timetravel;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class TimeTravelListener implements Listener {
    private final TimeTravelComponent timeTravel;

    protected TimeTravelListener(TimeTravelComponent timeTravel) {
        this.timeTravel = timeTravel;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        timeTravel.maybeUpdateOverride(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        timeTravel.maybeUpdateOverride(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        timeTravel.resetOverride(event.getPlayer());
    }
}
