package gg.packetloss.grindstone.admin;

import com.google.common.base.Joiner;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.util.List;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ShutdownCommands {
    private ShutdownComponent component;

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
            expectedDowntime = "30 seconds";
        }

        component.shutdown(sender instanceof Player ? (Player) sender : null, delay, expectedDowntime);
    }
}
