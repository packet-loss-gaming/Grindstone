package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "First Login", desc = "Get stuff the first time you come.")
@Depend(plugins = {"WorldGuard"})
public class FirstLoginComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;
    private List<Player> blockedPlayers = new ArrayList<>();

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20 * 30);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-lucky-diamond")
        public boolean luckyDiamond = true;
        @Setting("exit-region")
        public String exitRegion = "city-dung-exit";
        @Setting("first-teleport-x")
        public int firstTeleportX = 0;
        @Setting("first-teleport-y")
        public int firstTeleportY = 0;
        @Setting("first-teleport-z")
        public int firstTeleportZ = 0;
        @Setting("main-world")
        public String mainWorld = "City";
    }

    private WorldGuardPlugin getWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    @Override
    public void run() {

        // Region info
        try {
            World world = Bukkit.getWorld(config.mainWorld);
            try {
                ProtectedRegion protectedRegion = getWorldGuard().getRegionManager(world).getRegion(config.exitRegion);

                for (Player player : world.getPlayers()) {

                    if (LocationUtil.isBelowPlayer(world, protectedRegion, player)) {

                        // Remove teleport block
                        blockedPlayers.remove(player);

                        // Main player info
                        PlayerInventory inventory = player.getInventory();

                        // Declare Item Stacks
                        ItemStack[] startKit = new ItemStack[7];
                        startKit[0] = new ItemStack(ItemID.COOKED_BEEF, 32);
                        startKit[1] = new ItemStack(ItemID.STONE_PICKAXE);
                        startKit[2] = new ItemStack(ItemID.STONE_AXE);
                        startKit[3] = new ItemStack(ItemID.STONE_SHOVEL);
                        startKit[4] = new ItemStack(ItemID.STONE_SWORD);
                        startKit[5] = new ItemStack(ItemID.STONE_HOE);
                        startKit[6] = new ItemStack(ItemID.MAP);

                        // Tell others to great him/her
                        for (Player otherPlayer : server.getOnlinePlayers()) {
                            // Don't tell the player we are sending this message
                            if (otherPlayer != player) {
                                ChatUtil.sendNotice(otherPlayer, "Please welcome, "
                                        + player.getDisplayName() + " to the server.");
                            }
                        }

                        // Intro Messaging
                        ChatUtil.sendNotice(player, "Hello and welcome to Arrow Craft.");

                        // Give Items
                        ChatUtil.sendNotice(player, "Here have some free stuff. :D");
                        inventory.addItem(startKit);

                        // Greeting
                        ChatUtil.sendNotice(player, "Enjoy your time on Arrow Craft!");

                        // Surprise!
                        if (ChanceUtil.getChance(10) && config.luckyDiamond) {

                            // Give Items
                            inventory.addItem(new ItemStack(ItemID.DIAMOND, 1));

                            // Notify Player
                            ChatUtil.sendNotice(player, ChatColor.GOLD, "What's this, a diamond! You are very luck!");
                        }

                        player.teleport(world.getSpawnLocation());
                    }
                }
            } catch (Exception e) {
                log.warning("Please verify the region: " + config.exitRegion + " exists.");
            }
        } catch (Exception e) {
            log.warning("Please verify the world: " + config.mainWorld + " exists.");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if (blockedPlayers.contains(player)) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "Please walk through the stone doors into the teleport room.");
        }
    }

    @EventHandler
    public void onPlayerConnect(PlayerLoginEvent event) {

        if (event.getHostname().contains("arrowcraft") && event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {

            event.setKickMessage("Please use \"server.skelril.com\" from now on.");
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        // Main player info
        final Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();

        try {
            final World world = Bukkit.getWorld(config.mainWorld);
            ProtectedRegion protectedRegion
                    = getWorldGuard().getRegionManager(world).getRegion(config.exitRegion).getParent();

            if (!player.hasPlayedBefore() || LocationUtil.isInRegion(world, protectedRegion, player)) {

                server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                    @Override
                    public void run() {

                        try {

                            player.teleport(new Location(world, config.firstTeleportX, config.firstTeleportY,
                                    config.firstTeleportZ));

                            ChatUtil.sendNotice(player, "This is the Arrow Craft Dungeon.");
                            ChatUtil.sendNotice(player, "Don't break the rules and you will never see it again. :)");
                            ChatUtil.sendNotice(player, "To leave walk through the stone piston door and wait for the" +
                                    " " +
                                    "teleport system" +
                                    ".");
                            if (!blockedPlayers.contains(player)) blockedPlayers.add(player);
                        } catch (Exception e) {
                            log.warning("Please ensure the following location exists: "
                                    + config.firstTeleportX + ", " + config.firstTeleportY
                                    + ", " + config.firstTeleportZ + " in the world: " + config.mainWorld + ".");
                        }
                    }
                }, 20);
            }

            // Surprise!
            if (ChanceUtil.getChance(1, 1000) && config.luckyDiamond && inst.hasPermission(player,
                    "aurora.loginkit.diamond")) {

                // Give Items
                inventory.addItem(new ItemStack(ItemID.DIAMOND, 1));

                // Notify Player
                ChatUtil.sendNotice(player, ChatColor.GOLD + "What's this, a diamond! You are very luck!");
            }
        } catch (Exception e) {
            log.warning("Please ensure the following region exists: "
                    + config.exitRegion + " and has a parent in the world: " + config.mainWorld + ".");
        }
    }

}