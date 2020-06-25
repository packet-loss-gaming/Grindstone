package gg.packetloss.grindstone.world.timetravel;

import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

@CommandContainer
public class TimeTravelCommands {
    private TimeTravelComponent component;

    public TimeTravelCommands(TimeTravelComponent component) {
        this.component = component;
    }

    @Command(name = "timetravel", desc = "Change the current time travel focus")
    public void timeTravel(Player player, @Arg(desc = "version") ManagedWorldTimeContext version) {
        if (component.canUseTimeTravelCommand(player)) {
            component.setOverride(player, version);
            ChatUtil.sendNotice(player, "Portal time period set.");
        } else {
            ChatUtil.sendError(player, "You must return to the city to change your time travel settings.");
        }
    }
}
