/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockData;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.events.entity.ProjectileTickEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

@ComponentInformation(friendlyName = "Gone Fishing", desc = "Awesome fishing stuff.")
public class FishingComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;

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

        @Setting("enable-native-american-crops")
        public boolean enableNACrops = true;
        @Setting("enable-rare-catches")
        public boolean enableRareCatches = true;
        @Setting("enable-arrow-fishing")
        public boolean enableArrowFishing = true;
        @Setting("minimum-bow-force")
        public double minBowForce = .85;
        @Setting("fish-drop-chance")
        public int fishDropChance = 16;
    }

    // Native American Crops
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Basic Checks
        if (!config.enableNACrops || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (EnvironmentUtil.isCropBlock(block.getType())
                && player.getItemInHand().getTypeId() == ItemID.RAW_FISH) {

            // Get needed data for a cycle
            int type = block.getTypeId();
            int data = block.getData();
            int newData = BlockData.cycle(type, data, 1);

            // Check growth progress and adjust accordingly
            if (newData <= data) {
                ChatUtil.sendError(player, "Fish can no longer help this crop.");
                return;
            }

            // GROW!!! :D
            BlockState state = block.getState();
            state.setTypeId(type);
            state.setRawData((byte) newData);

            BlockGrowEvent blockGrowEvent = new BlockGrowEvent(block, state);
            server.getPluginManager().callEvent(blockGrowEvent);

            if (!blockGrowEvent.isCancelled()) {

                // Take Fish
                // ### BEGIN WORK AROUND ###
                final int amt = player.getItemInHand().getAmount();
                server.getScheduler().runTaskLater(inst, () -> {
                    if (amt > 1) {
                        player.setItemInHand(new ItemStack(ItemID.RAW_FISH, amt - 1));
                    } else {
                        player.setItemInHand(null);
                    }
                }, 1);
                // ### END WORK AROUND ###

                //player.getInventory().removeItem(new ItemStack(ItemID.RAW_FISH));

                // Update Block State
                state.update(true);
                ChatUtil.sendNotice(player, ChatColor.GREEN, "You use some fish to grow the crop!");
            }
        }
    }

    // Rare Catches
    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {

        Player player = event.getPlayer();
        World world = player.getWorld();

        // Basic Checks
        if (!config.enableRareCatches
                || !player.hasPermission("aurora.fishing.rare")
                || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        // Entity Check
        Location entityLoc = event.getHook().getLocation();

        // Drop Boolean
        boolean[] fishingDrops = new boolean[6];
        fishingDrops[0] = ChanceUtil.getChance(7);
        fishingDrops[1] = ChanceUtil.getChance(8);
        fishingDrops[2] = ChanceUtil.getChance(15);
        fishingDrops[3] = ChanceUtil.getChance(25);
        fishingDrops[4] = ChanceUtil.getChance(35);
        fishingDrops[5] = ChanceUtil.getChance(50);

        // Monster Boolean
        boolean[] fishingMonsters = new boolean[3];
        fishingMonsters[0] = ChanceUtil.getChance(15);
        fishingMonsters[1] = ChanceUtil.getChance(15);
        fishingMonsters[2] = ChanceUtil.getChance(15);

        // Execute
        String drop = null;
        if (fishingDrops[5]) {
            ItemStack diamond = new ItemStack(Material.DIAMOND, 1);
            drop = "a diamond";
            world.dropItemNaturally(entityLoc, diamond);
        } else if (fishingDrops[4]) {
            ItemStack goldNug = new ItemStack(Material.GOLD_INGOT,
                    (ChanceUtil.getRandom(16) * ChanceUtil.getRandom(4)));
            drop = "some gold nugget(s)";
            world.dropItemNaturally(entityLoc, goldNug);
        } else if (fishingDrops[3]) {
            ItemStack gold = new ItemStack(Material.GOLD_INGOT, 1);
            drop = "a gold ingot";
            world.dropItemNaturally(entityLoc, gold);
        } else if (fishingDrops[2]) {
            ItemStack iron = new ItemStack(Material.IRON_INGOT, 1);
            drop = "a iron ingot";
            world.dropItemNaturally(entityLoc, iron);
        } else if (fishingDrops[1]) {
            ItemStack bone = new ItemStack(Material.BONE, ChanceUtil.getRandom(12));
            drop = "some bones";
            world.dropItemNaturally(entityLoc, bone);
        } else if (fishingDrops[0]) {
            ItemStack wood = new ItemStack(Material.WOOD, ChanceUtil.getRandom(5));
            drop = "some wood";
            world.dropItemNaturally(entityLoc, wood);
        }

        // Grab our monsters >=]
        String creature = null;
        if (fishingMonsters[2] && EnvironmentUtil.isNightTime(world.getTime())) {
            creature = "creeper(s)";
            for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
                world.spawn(entityLoc, Creeper.class);
            }
        } else if (fishingMonsters[1] && EnvironmentUtil.isNightTime(world.getTime())) {
            creature = "skeleton(s)";
            for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
                Skeleton skeleton = world.spawn(entityLoc, Skeleton.class);
                EntityEquipment equipment = skeleton.getEquipment();
                equipment.setItemInHand(new ItemStack(ItemID.BOW));
            }
        } else if (fishingMonsters[0] && EnvironmentUtil.isNightTime(world.getTime())) {
            creature = "zombie(s)";
            for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
                world.spawn(entityLoc, Zombie.class);
            }
        }

        if ((creature != null) && (drop != null)) {
            player.sendMessage(ChatColor.DARK_GREEN + "You found " +
                    drop + " and some " + creature + "!");
        } else if (creature != null) {
            player.sendMessage(ChatColor.RED + "You found some " + creature + "!");
        } else if (drop != null) {
            player.sendMessage(ChatColor.GREEN + "You found " + drop + "!");
        }
    }

    // Stack-o-fish
    private final static ItemStack fishy = new ItemStack(Material.RAW_FISH, 1);

    @EventHandler
    public void onProjectileTick(ProjectileTickEvent event) {

        if (!config.enableArrowFishing || !(event.getEntity() instanceof Arrow)) return;

        // Was 3 ticks is now 1 tick
        if (ChanceUtil.getChance(3) || event.hasLaunchForce() && event.getLaunchForce() < config.minBowForce) return;

        Arrow arrow = (Arrow) event.getEntity();
        Location loc = arrow.getLocation();

        int dropFish = config.fishDropChance;

        if (event.hasLaunchForce()) {
            dropFish = ChanceUtil.getRandom(dropFish);
        } else {
            dropFish = ChanceUtil.getRandom(dropFish * dropFish);
        }

        if (ChanceUtil.getChance(dropFish) && EnvironmentUtil.isWater(loc.getBlock())) {
            ItemStack fish;

            // God fish?
            if (ChanceUtil.getChance(250)) {
                fish = CustomItemCenter.build(CustomItems.GOD_FISH);
            } else {
                fish = fishy.clone();
            }

            // Fish type
            if (ChanceUtil.getChance(40)) {
                fish.setDurability((short) 3);
            } else if (ChanceUtil.getChance(5)) {
                fish.setDurability((short) ChanceUtil.getRandom(2));
            }

            arrow.getWorld().dropItem(loc, fish);
        }
    }
}