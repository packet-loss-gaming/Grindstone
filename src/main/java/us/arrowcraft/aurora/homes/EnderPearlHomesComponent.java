package us.arrowcraft.aurora.homes;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import us.arrowcraft.aurora.events.HomeTeleportEvent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Ender Pearl Homes", desc = "Teleport with enderpearls!")
public class EnderPearlHomesComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private HomeDatabase homeDatabase;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        File homeDirectory = new File(inst.getDataFolder().getPath() + "/home");
        if (!homeDirectory.exists()) homeDirectory.mkdir();

        homeDatabase = new CSVHomeDatabase("homes", homeDirectory);
        homeDatabase.load();
    }

    public Location getBedLocation(Player player) {

        if (homeDatabase.houseExist(player.getName())) {
            Home home = homeDatabase.getHouse(player.getName());

            Location bedLoc = home.getLocation();
            Location betterLoc = LocationUtil.findFreePosition(bedLoc);

            if (betterLoc != null && bedLoc != betterLoc) bedLoc = betterLoc;
            return bedLoc;
        }
        return null;
    }

    public Location getRespawnLocation(Player player) {

        Location respawnLoc = player.getWorld().getSpawnLocation();
        if (getBedLocation(player) != null) return getBedLocation(player);
        return respawnLoc;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        Entity ent = event.getEntity();

        if (ent instanceof Enderman) {
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL, ChanceUtil.getRandom(12)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        final Player player = event.getPlayer();
        Location to = event.getTo();

        if (!inst.hasPermission(player, "aurora.home.enderpearl")
                || !event.getCause().equals(TeleportCause.ENDER_PEARL)) return;
        try {
            if (to.distanceSquared(player.getLocation()) < 1.5 * 1.5) {
                ChatUtil.sendNotice(player, "A powerful vortex sucks you up!");
                if (!homeDatabase.houseExist(player.getName())
                        || homeDatabase.getHouse(player.getName()).getLocation().getBlock().getTypeId()
                        != BlockID.BED) {
                    ChatUtil.sendNotice(player, "The vortex cannot find your home and sends you to spawn.");
                    event.setCancelled(true);
                    server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                        @Override
                        public void run() {

                            player.teleport(player.getWorld().getSpawnLocation());
                        }
                    }, 1);
                } else {
                    ChatUtil.sendNotice(player, "The vortex sends you to your home.");
                    event.setCancelled(true);
                    server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                        @Override
                        public void run() {

                            HomeTeleportEvent HTE = new HomeTeleportEvent(player, getBedLocation(player));
                            server.getPluginManager().callEvent(HTE);
                            if (!HTE.isCancelled()) player.teleport(HTE.getDestination());
                        }
                    }, 1);
                }
            }
        } catch (Exception e) {
            log.warning("The player: " + player.getName() + "'s teleport could not be processed by the: "
                    + this.getInformation().friendlyName() + " component.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        HomeTeleportEvent HTE = new HomeTeleportEvent(event.getPlayer(), getBedLocation(event.getPlayer()));
        server.getPluginManager().callEvent(HTE);
        if (!HTE.isCancelled()) event.setRespawnLocation(HTE.getDestination());
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {

        Player player = event.getPlayer();
        Location bedLoc = event.getBed().getLocation();

        if (player == null || bedLoc == null) return;
        if (player.getWorld().getName().toLowerCase().contains("legit")) return;

        boolean overWritten = false;

        if (homeDatabase.houseExist(player.getName())) {
            homeDatabase.deleteHouse(player.getName());
            overWritten = homeDatabase.save();
        }

        homeDatabase.saveHouse(player, bedLoc.getWorld().getName(), bedLoc.getBlockX(), bedLoc.getBlockY(),
                bedLoc.getBlockZ());
        if (homeDatabase.save()) {
            if (!overWritten) ChatUtil.sendNotice(player, "Your bed location has been set.");
            else ChatUtil.sendNotice(player, "Your bed location has been changed.");
        }
    }
}