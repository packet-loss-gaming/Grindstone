package us.arrowcraft.aurora;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Safe Tele", desc = "No falling pl0x.")
public class SafeTeleComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    private static final List<PlayerTeleportEvent.TeleportCause> causes = new ArrayList<>(2);

    static {
        causes.add(PlayerTeleportEvent.TeleportCause.COMMAND);
        causes.add(PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        if (causes.contains(event.getCause()) && !event.getPlayer().isFlying()) {
            Location loc = LocationUtil.findFreePosition(event.getTo());
            if (loc == null) {
                ChatUtil.sendError(event.getPlayer(), "That location is not safe!");
                event.setCancelled(true);
                return;
            }
            event.setTo(loc);
        }
    }
}
