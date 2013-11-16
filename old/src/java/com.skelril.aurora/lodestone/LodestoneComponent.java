package com.skelril.aurora.lodestone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.events.SignTeleportEvent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Lodestone", desc = "Manages all lodestones")
@Depend(components = SessionComponent.class)
public class LodestoneComponent extends BukkitComponent implements Listener {


    @InjectComponent
    private SessionComponent sessions;
    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //CommandBook.registerEvents(this);
    }

    // Player Management
    protected void activateSignTeleport(Player player, String destination) {

        LodestoneState session = sessions.getSession(LodestoneState.class, player);
        setTeleportSignDestination(player, destination);
        session.setTeleportState(true);
    }

    protected boolean isSignTeleportActive(Player player) {

        return sessions.getSession(LodestoneState.class, player).getTeleportSignState();
    }

    protected void deactivateSignTeleport(Player player) {

        LodestoneState session = sessions.getSession(LodestoneState.class, player);
        setTeleportSignDestination(player, "");
        session.setTeleportState(false);
    }

    protected void setTeleportSignDestination(Player player, String destination) {

        LodestoneState session = sessions.getSession(LodestoneState.class, player);
        session.setTeleportSignDestination(destination);
    }

    protected String getSignTeleportDestination(Player player) {

        return sessions.getSession(LodestoneState.class, player).getTeleportSignDestination();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        // Basic Checks
        if (action != Action.RIGHT_CLICK_BLOCK)
            return;
        if (block.getTypeId() != BlockID.WALL_SIGN)
            return;
        Sign sign = (Sign) block.getState();

        // See if this is a bank sign
        if (sign.getLine(1).equalsIgnoreCase(ChatColor.BLUE + "[Lodestone]") && sign.getLine(3).equals("Teleport")
                && !isSignTeleportActive(player)) {
            //TODO Array of valid teleports
            if (sign.getLine(2).equals("THE BANK")
                    || sign.getLine(2).equals("FLINT")
                    || sign.getLine(2).equals("FORT")
                    || sign.getLine(2).equals("SKY CITY")
                    || sign.getLine(2).equals("MUSHROOM ISLE")) {
                activateSignTeleport(player, sign.getLine(2));
            } else {
                return;
            }

            // Tell the player
            ChatUtil.sendNotice(player, "Teleport to: " + sign.getLine(2) + " activated!");
            ChatUtil.sendNotice(player, "Use a portal to proceed.");
        } else if (sign.getLine(1).equalsIgnoreCase(ChatColor.BLUE + "[Lodestone]") && sign.getLine(3).equals
                ("Teleport")) {
            deactivateSignTeleport(player);

            // Tell the player
            ChatUtil.sendNotice(player, "Teleport deactivated!");
            ChatUtil.sendNotice(player, "Portals have returned to their previous function.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {

        Player player = event.getPlayer();
        int blockTypeId = event.getBlock().getTypeId();

        // Check Block Type & see if we need to worry about it
        if (blockTypeId == BlockID.WALL_SIGN && event.getLine(1).equalsIgnoreCase("[Lodestone]")) {
            switch (event.getLine(3).toLowerCase()) {
                case "teleport":
                    // Check Permissions
                    if (!inst.hasPermission(player, "aurora.teleport.create"))
                        return;

                    // Setup the sign
                    event.setLine(0, "Right Click");
                    event.setLine(1, ChatColor.BLUE + "[Lodestone]");
                    event.setLine(2, event.getLine(2).toUpperCase());
                    event.setLine(3, "Teleport");

                    // Tell the player
                    ChatUtil.sendNotice(player, "Portal activator to: " + event.getLine(2) + " created!");
                    break;
                default:
                    event.setCancelled(true);
                    event.getBlock().breakNaturally(new ItemStack(ItemID.SIGN, 1));
                    ChatUtil.sendError(player, "That is not a valid Lodestone type.");
                    break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPortal(PlayerPortalEvent event) {

        final Player player = event.getPlayer();

        try {
            if ((event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
                    || event.getCause().equals(PlayerTeleportEvent.TeleportCause.END_PORTAL))
                    && isSignTeleportActive(player)) {

                SignTeleportEvent signTeleportEvent = new SignTeleportEvent(player, event.getFrom(),
                        getSignTeleportDestination(player),
                        LocationUtil.matchLocationFromText(getSignTeleportDestination(player)),
                        event.getCause(), event);

                Bukkit.getServer().getPluginManager().callEvent(signTeleportEvent);

                if (!signTeleportEvent.isCancelled()) {
                    event.useTravelAgent(signTeleportEvent.useTravelAgent());
                    event.setTo(signTeleportEvent.getToLocation());
                    event.setFrom(signTeleportEvent.getFromLocation());
                    if (signTeleportEvent.getDeactivationAfterUse()) {

                        // Schedule a new task so we don't set the new location to null
                        server.getScheduler().runTaskLater(inst, new Runnable() {

                            @Override
                            public void run() {

                                deactivateSignTeleport(player);
                            }
                        }, 1);
                    }
                }
            }
        } catch (Exception e) {
            ChatUtil.sendError(player, "An error has occurred.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSignPortal(SignTeleportEvent event) {
        // Basic Check
        if (event.getToLocationName().equalsIgnoreCase("FLINT")
                || event.getToLocationName().equalsIgnoreCase("SKY CITY"))
            event.setUseTravelAgent(false);
    }

    // Session Manager
    private static class LodestoneState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(7);

        @Setting("teleport-sign-state")
        private boolean teleportSignState = false;
        @Setting("teleport-sign-destination")
        private String teleportSignDestination;

        protected LodestoneState() {

            super(MAX_AGE);
        }

        public boolean getTeleportSignState() {

            return teleportSignState;
        }

        public void setTeleportState(boolean teleportSignState) {

            this.teleportSignState = teleportSignState;
        }

        public String getTeleportSignDestination() {

            return teleportSignDestination;
        }

        public void setTeleportSignDestination(String destination) {

            this.teleportSignDestination = destination;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}