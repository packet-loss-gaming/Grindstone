package us.arrowcraft.aurora;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import us.arrowcraft.aurora.events.FallBlockerEvent;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */

@ComponentInformation(friendlyName = "Soft Wool", desc = "Fall softly my friends.")
public class SoftWoolComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();
        EntityDamageEvent.DamageCause damageCause = event.getCause();

        if (damageCause.equals(EntityDamageEvent.DamageCause.FALL)
                && LocationUtil.getBelowID(entity.getLocation(), BlockID.CLOTH)) {
            if (entity instanceof Player) {
                server.getPluginManager().callEvent(new FallBlockerEvent((Player) entity));
                ChatUtil.sendNotice((Player) entity, "The cloth negates your fall damage.");
            }
            event.setCancelled(true);
        }
    }
}
