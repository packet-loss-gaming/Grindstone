/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Grounder", desc = "GameMode enforcement to a new level.")
public class GrounderComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {

        Player player = event.getPlayer();
        GameMode gameMode = event.getNewGameMode();

        // Check for the gamemode changing from GameMode.SURVIVAL to GameMode.CREATIVE
        if (gameMode.equals(GameMode.CREATIVE)) {
            if (!inst.hasPermission(player, "aurora.gamemode.creative.permit")) {

                // Stop this change & notify
                event.setCancelled(true);
                ChatUtil.sendWarning(player, "Your gamemode change has been denied.");
                log.info("The player: " + player.getName() + " was stopped from changing gamemodes.");
            }
        }
    }
}