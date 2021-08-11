/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.google.common.base.Joiner;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.util.List;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ShutdownCommands {
    private final ShutdownComponent component;

    public ShutdownCommands(ShutdownComponent component) {
        this.component = component;
    }

    @Command(name = "shutdown", desc = "Used to restart the server")
    @CommandPermissions({"aurora.admin.server.shutdown"})
    public void shutdownCmd(
            CommandSender sender,
            @Arg(desc = "Number of seconds before shutdown", def = "60") int delay,
            @Arg(desc = "How long the server will be down", def = "", variable = true) List<String> expectedDowntimeArgs) {

        String expectedDowntime = Joiner.on(' ').join(expectedDowntimeArgs);
        if (expectedDowntimeArgs.isEmpty()) {
            expectedDowntime = ShutdownComponent.DEFAULT_DOWN_TIME;
        }

        component.shutdown(sender instanceof Player ? (Player) sender : null, delay, expectedDowntime);
    }

    @Command(name = "idleshutdown", desc = "Used to restart the server when all players have disconnected")
    @CommandPermissions({"aurora.admin.server.shutdown"})
    public void idleShutdownCmd(CommandSender sender) {
        component.idleShutdown();
        ChatUtil.sendNotice(sender, "The server will shutdown 30 seconds after all players have stayed disconnected.");
    }
}
