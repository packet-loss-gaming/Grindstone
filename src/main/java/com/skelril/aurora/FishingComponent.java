package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockData;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Gone Fishing", desc = "Awesome fishing stuff.")
public class FishingComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;
    private Map<Integer, Integer> arrowTaskId = new HashMap<>();
    private Map<Integer, String> arrowLoc = new HashMap<>();

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
            byte data = block.getData();
            byte newData = (byte) BlockData.cycle(type, data, 1);

            while (newData < BlockData.cycle(type, newData, 1)) {
                newData = (byte) BlockData.cycle(type, newData, 1);
            }

            // Check growth progress and adjust accordingly
            if (((int) newData) <= ((int) data) || ((int) data) >= 7) {
                ChatUtil.sendError(player, "Fish can no longer help this crop.");
            } else {
                // GROW!!! :D
                BlockState state = block.getState();
                state.setTypeId(type);
                state.setRawData(newData);

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
        fishingDrops[0] = ChanceUtil.getChance(1, 25);
        fishingDrops[1] = ChanceUtil.getChance(1, 35);
        fishingDrops[2] = ChanceUtil.getChance(1, 50);
        fishingDrops[3] = ChanceUtil.getChance(1, 75);
        fishingDrops[4] = ChanceUtil.getChance(1, 100);
        fishingDrops[5] = ChanceUtil.getChance(1, 500);

        // Monster Boolean
        boolean[] fishingMonsters = new boolean[3];
        fishingMonsters[0] = ChanceUtil.getChance(1, 200);
        fishingMonsters[1] = ChanceUtil.getChance(1, 400);
        fishingMonsters[2] = ChanceUtil.getChance(1, 500);

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

    // Arrow Fishing
    private void addArrowTaskId(Arrow arrow, int taskId) {

        arrowTaskId.put(arrow.getEntityId(), taskId);
    }

    private void addArrowLoc(Arrow arrow, String loc) {

        arrowLoc.put(arrow.getEntityId(), loc);
    }

    private void removeArrow(Arrow arrow) {

        if (arrowTaskId.containsKey(arrow.getEntityId())) {
            arrowTaskId.remove(arrow.getEntityId());
        }

        if (arrowLoc.containsKey(arrow.getEntityId())) {
            arrowLoc.remove(arrow.getEntityId());
        }

    }

    private int getArrowTaskId(Arrow arrow) {

        int getArrowTaskId = 0;
        if (arrowTaskId.containsKey(arrow.getEntityId())) {
            getArrowTaskId = arrowTaskId.get(arrow.getEntityId());
        }
        return getArrowTaskId;
    }

    private boolean arrowLocChanged(String loc) {

        return !arrowLoc.containsValue(loc);
    }

    // Arrow Fishing
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowFire(ProjectileLaunchEvent event) {

        // Basic Check
        if (!config.enableArrowFishing) return;
        if (event.getEntityType() != EntityType.ARROW) return;
        final Arrow arrow = (Arrow) event.getEntity();

        int taskId = server.getScheduler().scheduleSyncRepeatingTask(inst, new Runnable() {

            @Override
            public void run() {

                Location loc = arrow.getLocation();

                if (arrow.isDead() || (!(arrowLocChanged(loc.toString())))) {
                    int id = getArrowTaskId(arrow);

                    if (id != -1) {
                        removeArrow(arrow);
                        server.getScheduler().cancelTask(id);
                    }
                } else {
                    int blockTypeId = arrow.getWorld().getBlockTypeIdAt(loc);
                    boolean dropFish;
                    if (arrow.getShooter() instanceof Player) {
                        dropFish = inst.hasPermission((Player) arrow.getShooter(),
                                "aurora.fishing.arrow.master") ? ChanceUtil.getChance(1, 4) : ChanceUtil.getChance(1,
                                16);
                    } else {
                        dropFish = ChanceUtil.getChance(1, 256);
                    }

                    addArrowLoc(arrow, loc.toString());
                    if (dropFish && ((blockTypeId == BlockID.WATER) || (blockTypeId == BlockID.STATIONARY_WATER))) {
                        arrow.getWorld().dropItemNaturally(loc, new ItemStack(Material.RAW_FISH, 1));
                    }
                }
            }
        }, 0L, 3L); // Start at 0 ticks and repeat every 3 ticks
        addArrowTaskId(arrow, taskId);
    }
}