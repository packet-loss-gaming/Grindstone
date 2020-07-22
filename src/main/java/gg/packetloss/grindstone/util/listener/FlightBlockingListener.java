/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.listener;

import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.function.Predicate;

public class FlightBlockingListener implements Listener {
    private final Predicate<Player> appliesTo;

    public FlightBlockingListener(Predicate<Player> appliesTo) {
        this.appliesTo = appliesTo;
    }

    public FlightBlockingListener(AdminComponent admin, Predicate<Player> appliesTo) {
        this((p) -> appliesTo.test(p) && !admin.isAdmin(p));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) {
            return;
        }

        Player player = event.getPlayer();
        if (GeneralPlayerUtil.hasFlyingGamemode(player)) {
            return;
        }

        if (!appliesTo.test(player)) {
            return;
        }

        player.setAllowFlight(false);
        event.setCancelled(true);

        ChatUtil.sendError(player, "You cannot fly here!");
    }
}
