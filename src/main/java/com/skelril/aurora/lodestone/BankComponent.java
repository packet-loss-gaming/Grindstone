package com.skelril.aurora.lodestone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.skelril.aurora.events.SignTeleportEvent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

import static com.skelril.aurora.util.LocationUtil.isInRegion;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Bank", desc = "Manage the bank.")
@Depend(components = SessionComponent.class, plugins = "WorldGuard")
public class BankComponent extends BukkitComponent implements Listener {

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private LodestoneComponent lodestoneComponent;
    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //CommandBook.registerEvents(this);
    }

    private WorldGuardPlugin getWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    // Player Management
    protected void sendToBank(Player player, Location loc) {

        BankState session = sessions.getSession(BankState.class, player);
        session.setLastLoc(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        ChatUtil.sendNotice(player, "You have entered the bank!");
    }

    protected boolean isInBank(Player player) {

        return LocationUtil.isInRegion(player.getWorld(), getWorldGuard().getGlobalRegionManager().get(player
                .getWorld())
                .getRegion("bank"), player);
    }

    protected boolean isInBank(Location location) {

        return isInRegion(location.getWorld(), getWorldGuard().getGlobalRegionManager().get(location.getWorld()
        ).getRegion("bank"), location);
    }

    protected Location getLastLocation(Player player) {

        return sessions.getSession(BankState.class, player).getLastLoc();
    }

    protected void sendBackFromBank(final Player player) {

        final BankState session = sessions.getSession(BankState.class, player);
        // Schedule a new task so we don't set the new location to null
        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                ChatUtil.sendNotice(player, "You have left the bank!");
                session.setLastLoc(null);
            }
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        final Player player = event.getPlayer();

        if (!(event.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND)
                || event.getCause().equals(PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
                || event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN)))
            return;

        if (!event.getFrom().getWorld().getName().equals("Destrio"))
            return;

        try {
            if (!isInBank(event.getTo()) && isInBank(event.getFrom())) {
                sendBackFromBank(player);
                lodestoneComponent.deactivateSignTeleport(player);

            } else if (isInBank(event.getTo()) && !isInBank(event.getFrom())) {
                sendToBank(player, event.getFrom());
                lodestoneComponent.activateSignTeleport(player, "THE BANK");
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignTeleport(SignTeleportEvent event) {

        final Player player = event.getPlayer();

        // Basic Check
        if (!event.getToLocationName().equalsIgnoreCase("THE BANK"))
            return;
        if (!event.getFromLocation().getWorld().getName().equals("Destrio"))
            return;

        // Do we care?
        try {
            if (!isInBank(player)) {

                // Fire the TA!
                event.setUseTravelAgent(false);

                // Don't deactivate
                event.setDeactivateAfterUse(false);

                // Redirect the teleport (The Nether is not the bank)
                event.setToLocation(LocationUtil.matchLocationFromText("THE BANK"));

                // Tell the Player Manager
                sendToBank(player, event.getFromLocation());
            } else if (isInBank(player)) {

                // Fire the TA!
                event.setUseTravelAgent(false);

                // Redirect the teleport
                try {
                    event.setToLocation(getLastLocation(player));
                } catch (Exception e) {
                    event.setToLocation(event.getFromLocation().getWorld().getSpawnLocation());
                    ChatUtil.sendError(player, "Your last location could not be found!");
                }

                // Tell the Player Manager
                sendBackFromBank(player);
            } else {
                event.setCancelled(true);
                ChatUtil.sendError(player, "The bank could not determine your location!");
            }
        } catch (Exception e) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "WorldGuard regions are not active!");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (!player.getWorld().getName().equals("Destrio"))
            return;

        // Thought you'd grief the bank ey?
        try {
            if (isInBank(player)) {
                event.setCancelled(true);
                player.damage(1);
                ChatUtil.sendWarning(player, "Don't do that!");
            }
        } catch (Exception e) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "WorldGuard regions are not active!");
        }
    }

    // Session Manager
    private static class BankState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(7);

        private Location lastLoc = null;

        protected BankState() {

            super(MAX_AGE);
        }

        public void setLastLoc(Location lastLoc) {

            this.lastLoc = lastLoc == null ? null : lastLoc.clone();
        }

        public Location getLastLoc() {

            return lastLoc;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}