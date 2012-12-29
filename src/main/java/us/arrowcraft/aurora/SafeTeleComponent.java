package us.arrowcraft.aurora;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import us.arrowcraft.aurora.util.LocationUtil;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Safe Tele", desc = "No falling pl0x.")
public class SafeTeleComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {

        if (!event.getPlayer().isFlying()) event.setTo(LocationUtil.getGround(event.getTo()));
    }
}
