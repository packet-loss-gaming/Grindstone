/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.*;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "First Login", desc = "Get stuff the first time you come.")
@Depend(plugins = {"WorldGuard"})
public class FirstLoginComponent extends BukkitComponent implements Listener {

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
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-lucky-diamond")
        public boolean luckyDiamond = true;
        @Setting("first.region")
        public String firstRegion = "city-dung";
        @Setting("first.teleport.x")
        public int firstTeleportX = 0;
        @Setting("first.teleport.y")
        public int firstTeleportY = 0;
        @Setting("first.teleport.z")
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

    @EventHandler
    public void onSafeFall(FallBlockerEvent event) {

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (blockedPlayers.contains(event.getPlayer())) {

            // Stop the fall message
            event.setDisplayMessage(false);

            // Stop any thunderstorms
            world.setThundering(false);

            // Kill All mobs
            Collection<Entity> zombies = world.getEntitiesByClasses(Creature.class);
            String customName;
            for (Entity entity : zombies) {
                if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) continue;
                customName = ((LivingEntity) entity).getCustomName();
                if (customName != null && customName.contains("Apocalyptic")) {
                    ((LivingEntity) entity).setHealth(0);
                }
            }

            // Remove teleport block
            blockedPlayers.remove(player);

            // Main player info
            PlayerInventory inventory = player.getInventory();

            // Declare Item Stacks
            ItemStack[] startKit = new ItemStack[]{
                    // BookUtil.Tutorial.newbieBook(),
                    new ItemStack(ItemID.COOKED_BEEF, 32),
                    new ItemStack(ItemID.STONE_PICKAXE),
                    new ItemStack(ItemID.STONE_AXE),
                    new ItemStack(ItemID.STONE_SHOVEL),
                    new ItemStack(ItemID.STONE_SWORD),
                    new ItemStack(ItemID.STONE_HOE),
                    new ItemStack(ItemID.MAP),
                    new ItemStack(ItemID.BED_ITEM),
                    CustomItemCenter.build(CustomItems.GEM_OF_LIFE, 3)
            };

            // Tell others to great him/her
            for (Player otherPlayer : server.getOnlinePlayers()) {
                // Don't tell the player we are sending this message
                if (otherPlayer != player) {
                    ChatUtil.sendNotice(otherPlayer, "Please welcome, " + player.getDisplayName() + " to the server.");
                }
            }

            // Greeting
            ChatUtil.sendNotice(player, "Welcome to the Packet Loss Gaming Minecraft server!");
            inventory.addItem(startKit);

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

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseSpawn(ApocalypsePersonalSpawnEvent event) {
        if (blockedPlayers.contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if (blockedPlayers.contains(player)) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "Please stick to the path.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        // Main player info
        final Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();

        try {
            final World world = Bukkit.getWorld(config.mainWorld);
            ProtectedRegion protectedRegion = getWorldGuard().getRegionManager(world).getRegion(config.firstRegion);

            if (!player.hasPlayedBefore() || LocationUtil.isInRegion(world, protectedRegion, player)) {

                server.getScheduler().scheduleSyncDelayedTask(inst, () -> {
                    try {

                        Location firstLoc = new Location(world, config.firstTeleportX, config.firstTeleportY,
                                config.firstTeleportZ);
                        firstLoc.setPitch(2);
                        player.teleport(firstLoc);

                        ChatUtil.sendNotice(player, "Welcome to Packet Loss Gaming Minecraft server!");
                        ChatUtil.sendNotice(player, "Follow the path to begin your adventure!");
                        if (!blockedPlayers.contains(player)) blockedPlayers.add(player);
                    } catch (Exception e) {
                        log.warning("Please ensure the following location exists: "
                                + config.firstTeleportX + ", " + config.firstTeleportY
                                + ", " + config.firstTeleportZ + " in the world: " + config.mainWorld + ".");
                    }
                }, 5);
            }
        } catch (Exception e) {
            log.warning("Please ensure the following region exists: "
                    + config.firstRegion + " and has a parent in the world: " + config.mainWorld + ".");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        if (blockedPlayers.contains(event.getPlayer())) blockedPlayers.remove(event.getPlayer());
    }

}