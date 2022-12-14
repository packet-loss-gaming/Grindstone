/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.task.promise.FailableTaskFuture;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class InviteCommands {
    private final PlayerInviteComponent playerInvite;

    public InviteCommands(PlayerInviteComponent playerInvite) {
        this.playerInvite = playerInvite;
    }

    @Command(name = "invite", desc = "Invite a player")
    @CommandPermissions({"aurora.invite.player"})
    public void invitePlayerCmd(Player player,
                                @Arg(name = "playerName", desc = "player to invite") String playerName) {
        ChatUtil.sendNotice(player, "Looking up player...");
        FailableTaskFuture.asyncTask(() -> {
            PlayerProfile profile = null;
            try {
                profile = Bukkit.getServer().createProfile(playerName);
                profile.complete(false, true);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (profile == null || !profile.isComplete()) {
                return TaskResult.failed();
            }
            return TaskResult.of(profile.getId());
        }).thenComposeFailable(
            (newPlayerID) -> {
                return playerInvite.invitePlayer(player.getUniqueId(), newPlayerID);
            },
            (ignored) -> {
                ChatUtil.sendError(player, "No player found to invite by that name.");
            }
        ).thenAccept(
            (inviteResult) -> {
                switch (inviteResult) {
                    case SUCCESS -> {
                        ChatUtil.sendNotice(player, "Player invited!");
                        ChatUtil.sendNotice(player, "By default they will be sent to your bed.");
                        ChatUtil.sendNotice(
                            player,
                            "You can set a default location with ",
                            Text.of(
                                ChatColor.BLUE,
                                "/setwelcomelocation",
                                TextAction.Click.runCommand("/setwelcomelocation")
                            ),
                            "."
                        );
                    }
                    case ALREADY_INVITED -> {
                        ChatUtil.sendError(player, "Someone already invited this player.");
                    }
                }
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
    }

    @Command(name = "setwelcomelocation", desc = "Set the welcome location for your invites")
    @CommandPermissions({"aurora.invite.player"})
    public void invitePlayerCmd(Player player) {
        boolean isUpdate = playerInvite.setInviteDestination(player.getUniqueId(), player.getLocation());
        if (isUpdate) {
            ChatUtil.sendNotice(player, "Your welcome location has been updated.");
        } else {
            ChatUtil.sendNotice(player, "Your welcome location has been set.");
        }
    }
}
