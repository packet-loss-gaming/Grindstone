/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

import com.google.common.base.Joiner;
import com.sk89q.commandbook.command.argument.OfflineSinglePlayerTarget;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.List;
import java.util.UUID;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class JailCommands {
    private JailComponent component;

    public JailCommands(JailComponent component) {
        this.component = component;
    }

    @Command(name = "jail", desc = "Jail a player")
    @CommandPermissions({"aurora.jail.jail"})
    public void jailCmd(CommandSender sender,
                        @ArgFlag(name = 't', desc = "duration to jail", def = "") String duration,
                        @ArgFlag(name = 'p', desc = "target prison", def = "") PrisonIdentifier prison,
                        @Switch(name = 'm', desc = "mute player") boolean mutePlayer,
                        @Switch(name = 's', desc = "jail silently") boolean jailSilently,
                        @Arg(desc = "target") OfflineSinglePlayerTarget target,
                        @Arg(desc = "reason", def = "", variable = true) List<String> reasonArgs) throws CommandException {
        // Get and validate prison
        String prisonName = component.getDefaultPrison();
        if (prison != null) {
            prisonName = prison.get();
        }

        // FIXME: This really shouldn't be necessary, but it's possible there are no prisons.
        if (!component.isPrison(prisonName)) {
            ChatUtil.sendError(sender, "No such prison exists.");
            return;
        }

        // Get and validate time
        long endDate = duration == null ? 0L : InputUtil.TimeParser.matchFutureDate(duration);

        // Get target player ID
        OfflinePlayer targetPlayer = target.get();
        UUID targetPlayerID = targetPlayer.getUniqueId();
        String targetPlayerName = targetPlayer.getName();

        String reason = Joiner.on(' ').join(reasonArgs);

        // Jail the player
        component.jail(targetPlayerID, prisonName, sender, reason, endDate, mutePlayer);

        if (!component.getInmateDatabase().save()) {
            throw new CommandException("Inmate database failed to save. See console.");
        }

        // Tell the sender of their success
        ChatUtil.sendNotice(sender, "The player: " + targetPlayerName + " has been jailed.");

        // Broadcast the Message
        if (!jailSilently) {
            component.broadcastJailing(sender, targetPlayer, reason);
        }
    }

    @Command(name = "unjail", desc = "Free a player from jail")
    @CommandPermissions({"aurora.jail.unjail"})
    public void unjailCmd(CommandSender sender,
                        @Switch(name = 's', desc = "unjail silently") boolean unjailSilently,
                        @Arg(desc = "target") OfflineSinglePlayerTarget target,
                        @Arg(desc = "reason", def = "", variable = true) List<String> reasonArgs) throws CommandException {
        // Get target player ID
        OfflinePlayer targetPlayer = target.get();
        UUID targetPlayerID = targetPlayer.getUniqueId();
        String targetPlayerName = targetPlayer.getName();

        String reason = Joiner.on(' ').join(reasonArgs);

        // Unjail the player
        if (!component.unjail(targetPlayerID, sender, reason)) {
            ChatUtil.sendError(sender, targetPlayerName + " was not jailed.");
            return;
        }

        if (!component.getInmateDatabase().save()) {
            throw new CommandException("Inmate database failed to save. See console.");
        }

        // Tell the sender of their success
        ChatUtil.sendNotice(sender, "The player: " + targetPlayerName + " has been unjailed!");

        // Broadcast the Message
        if (!unjailSilently) {
            component.broadcastUnjailing(sender, targetPlayer, reason);
        }
    }
}
