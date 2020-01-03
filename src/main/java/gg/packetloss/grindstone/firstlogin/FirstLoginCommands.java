package gg.packetloss.grindstone.firstlogin;

import org.bukkit.command.CommandSender;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;

@CommandContainer
public class FirstLoginCommands {
    private FirstLoginComponent component;

    public FirstLoginCommands(FirstLoginComponent component) {
        this.component = component;
    }

    @Command(name = "welcome", desc = "Display welcome information")
    public void teleportHome(CommandSender sender) {
        component.sendIntroText(sender);
    }
}
