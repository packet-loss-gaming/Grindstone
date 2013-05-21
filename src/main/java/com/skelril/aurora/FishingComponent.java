package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockData;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author Turtle9598
 */
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

        if (EnvironmentUtil.isCropBlock(block.getTypeId())
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
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        if (amt > 1) {
                            player.setItemInHand(new ItemStack(ItemID.RAW_FISH, amt - 1));
                        } else {
                            player.setItemInHand(null);
                        }
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
                || event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;

        // Entity Check
        Entity entity = event.getCaught();
        Location entityLoc = event.getCaught().getLocation();
        if (!(entity instanceof Item)) return;

        // Message Info

        // Drop Boolean
        boolean[] fishingDrops = new boolean[6];
        fishingDrops[0] = ChanceUtil.getChance(25);
        fishingDrops[1] = ChanceUtil.getChance(35);
        fishingDrops[2] = ChanceUtil.getChance(50);
        fishingDrops[3] = ChanceUtil.getChance(75);
        fishingDrops[4] = ChanceUtil.getChance(100);
        fishingDrops[5] = ChanceUtil.getChance(500);

        // Monster Boolean
        boolean[] fishingMonsters = new boolean[3];
        fishingMonsters[0] = ChanceUtil.getChance(75);
        fishingMonsters[1] = ChanceUtil.getChance(75);
        fishingMonsters[2] = ChanceUtil.getChance(75);

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
            for (int i = 1; i < ChanceUtil.getRandom(10); i++) {
                world.spawn(entityLoc, Creeper.class);
            }
        } else if (fishingMonsters[1] && EnvironmentUtil.isNightTime(world.getTime())) {
            creature = "skeleton(s)";
            for (int i = 1; i < ChanceUtil.getRandom(10); i++) {
                world.spawn(entityLoc, Skeleton.class);
            }
        } else if (fishingMonsters[0] && EnvironmentUtil.isNightTime(world.getTime())) {
            creature = "zombie(s)";
            for (int i = 1; i < ChanceUtil.getRandom(10); i++) {
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

            Player shooter = arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null;
            dropFish = ChanceUtil.getRandom(dropFish);

            if (shooter != null && inst.hasPermission(shooter, "aurora.fishing.arrow.master")) {
                dropFish = (int) Math.sqrt(dropFish);
            }
        } else {
            dropFish = ChanceUtil.getRandom(dropFish * dropFish);
        }

        if (ChanceUtil.getChance(dropFish) && EnvironmentUtil.isWater(loc.getBlock())) {
            arrow.getWorld().dropItemNaturally(loc, fishy);
        }
    }
}