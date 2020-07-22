/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.command.argument.MultiPlayerTarget;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.commandbook.util.entity.player.iterators.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.util.UUID;

@CommandContainer
public class WarpCommands {
    private WarpsComponent component;

    public WarpCommands(WarpsComponent component) {
        this.component = component;
    }

    @Command(name = "warp", desc = "Teleport to a warp")
    public void warp(CommandSender sender,
                     @Switch(name = 's', desc = "silent") boolean silent,
                     @Arg(desc = "Destination warp") WarpPoint warp,
                     @Arg(desc = "Player(s) to target", def = "") MultiPlayerTarget targetPlayers)
            throws CommandException {
        // Parse the targets
        if (targetPlayers == null) {
            targetPlayers = new MultiPlayerTarget(PlayerUtil.checkPlayer(sender));
        }

        // Check access permissions if the sender is a player
        if (sender instanceof Player) {
            WarpQualifiedName qualifiedName = warp.getQualifiedName();
            UUID warpNamespace = qualifiedName.getNamespace();

            // Check warp access, pretend it doesn't exist if permission is denied
            if (((Player) sender).getUniqueId().equals(warpNamespace)) {
                if (!sender.hasPermission("aurora.warp.access.self")) {
                    throw new WarpNotFoundException();
                }
            } else if (qualifiedName.isGlobal()) {
                if (!sender.hasPermission("aurora.warp.access.global")) {
                    throw new WarpNotFoundException();
                }
            } else {
                if (!sender.hasPermission("aurora.warp.access." + warpNamespace)) {
                    throw new WarpNotFoundException();
                }
            }

            // Check teleport access on targets
            for (Player target : targetPlayers) {
                if (target == sender) {
                    CommandBook.inst().checkPermission(sender, "aurora.warp.teleport.self");
                } else {
                    CommandBook.inst().checkPermission(sender, "aurora.warp.teleport.other");
                }
            }
        }

        Location loc = warp.getSafeLocation();
        (new TeleportPlayerIterator(sender, loc, silent)).iterate(targetPlayers);
    }

    @Command(name = "home", desc = "Go to your home")
    public void teleportHome(Player player) {
        CommandBook.server().dispatchCommand(player, "warp home");
    }
}
