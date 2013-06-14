package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.admin.AdminState;
import com.skelril.aurora.events.PlayerAdminModeChangeEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Wilderness Core", desc = "Operate the wilderness.")
@Depend(components = {AdminComponent.class})
public class WildernessCoreComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;

    private LocalConfiguration config;
    private ConcurrentHashMap<Location, TaskPool> taskPool = new ConcurrentHashMap<>();

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

    @Override
    public void run() {

        sync:
        {
            if (!config.enableSync) break sync;

            final World city = Bukkit.getWorld(config.cityWorld);
            final World wilderness = Bukkit.getWorld(config.wildernessWorld);
            boolean kill = false;

            if (city == null) {
                log.warning("Please verify the world: " + config.cityWorld + " exist.");
                kill = true;
            }
            if (wilderness == null) {
                log.warning("Please verify the world: " + config.wildernessWorld + " exist.");
                kill = true;
            }
            if (kill) break sync;

            // Time
            if (wilderness.getTime() != city.getTime()) {
                wilderness.setTime(city.getTime());
            }

            // Storm - General
            if (wilderness.hasStorm() != city.hasStorm()) {
                wilderness.setStorm(city.hasStorm());
            }

            if (wilderness.getWeatherDuration() != city.getWeatherDuration()) {
                wilderness.setWeatherDuration(city.getWeatherDuration());
            }

            // Storm - Thunder
            if (wilderness.isThundering() != city.isThundering()) {
                wilderness.setThundering(city.isThundering());
            }

            if (wilderness.getThunderDuration() != city.getThunderDuration()) {
                wilderness.setThunderDuration(city.getThunderDuration());
            }
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("city-world")
        public String cityWorld = "City";
        @Setting("wilderness-world")
        public String wildernessWorld = "Wilderness";
        @Setting("enable-sync")
        public boolean enableSync = true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {

        TravelAgent agent = event.getPortalTravelAgent();

        final Player player = event.getPlayer();
        final Location pLoc = player.getLocation().clone();
        final Location from = event.getFrom();
        final Location to = event.getTo();

        final World city = Bukkit.getWorld(config.cityWorld);
        final World wilderness = Bukkit.getWorld(config.wildernessWorld);
        final World wildernessNether = Bukkit.getWorld(config.wildernessWorld + "_nether");
        boolean kill = false;

        if (city == null) {
            log.warning("Please verify the world: " + config.cityWorld + " exist.");
            kill = true;
        }
        if (wilderness == null) {
            log.warning("Please verify the world: " + config.wildernessWorld + " exist.");
            kill = true;
        }
        if (wildernessNether == null) {
            log.warning("Please verify the world: " + config.wildernessWorld + "_nether exist.");
            kill = true;
        }
        if (kill) return;


        switch (event.getCause()) {
            case END_PORTAL:
                event.useTravelAgent(true);
                agent.setCanCreatePortal(false);
                event.setPortalTravelAgent(agent);
                if (from.getWorld().equals(city)) {
                    event.setTo(wilderness.getSpawnLocation());
                } else if (from.getWorld().equals(wilderness)) {
                    event.setTo(city.getSpawnLocation());
                }
                break;
            case NETHER_PORTAL:

                // Wilderness Code
                event.useTravelAgent(true);
                if (from.getWorld().equals(wilderness)) {
                    pLoc.setWorld(wildernessNether);
                    pLoc.setX(pLoc.getBlockX() / 8);
                    pLoc.setZ(pLoc.getBlockZ() / 8);
                    agent.setCanCreatePortal(true);
                    event.setPortalTravelAgent(agent);
                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                } else if (from.getWorld().getName().contains(config.wildernessWorld)) {
                    pLoc.setWorld(wilderness);
                    pLoc.setX(pLoc.getBlockX() * 8);
                    pLoc.setZ(pLoc.getBlockZ() * 8);
                    agent.setCanCreatePortal(true);
                    event.setPortalTravelAgent(agent);
                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                }

                // City Code
                if (from.getWorld().getName().contains(config.cityWorld)) {
                    event.setTo(LocationUtil.grandBank(city));
                    agent.setCanCreatePortal(false);
                    event.setPortalTravelAgent(agent);
                } else if (to.getWorld().getName().contains(config.cityWorld)) {
                    event.setTo(city.getSpawnLocation());
                    agent.setCanCreatePortal(false);
                    event.setPortalTravelAgent(agent);
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalForm(PortalCreateEvent event) {

        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) return;
        if (event.getWorld().getName().contains(config.cityWorld)) event.setCancelled(true);
    }

    @EventHandler
    public void onAdminModeChange(PlayerAdminModeChangeEvent event) {

        World world = event.getPlayer().getWorld();

        if (event.getNewAdminState().equals(AdminState.SYSOP)) return;
        if (!event.getNewAdminState().equals(AdminState.MEMBER) && world.getName().contains(config.wildernessWorld)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        if (event.getTo().getWorld() != event.getFrom().getWorld()) {

            check(event.getPlayer(), event.getTo().getWorld().getName());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

        check(player, player.getWorld().getName());
    }

    public void check(Player player, String to) {

        if (to.contains(config.wildernessWorld)) {

            adminComponent.deadmin(player);
        }
    }

    // Catch possible escapes
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.wildernessWorld)) adminComponent.deadmin(player);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {

        Player player = event.getPlayer();

        if (adminComponent.isAdmin(player)) return;
        if (player.getWorld().getName().contains(config.wildernessWorld) && event.isFlying()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        final BlockState block = event.getBlock().getState();

        if (!player.getWorld().getName().contains(config.wildernessWorld)) return;

        if (isEffectedOre(block.getTypeId())) {

            addPool(new BaseBlock(block.getTypeId()), block.getLocation());
        }

        /*
        if (isTree(block)) {

            Block treeBase = LocationUtil.getGround(block.getLocation()).getBlock();
            int testId = treeBase.getRelative(BlockFace.DOWN).getTypeId();

            if (testId == BlockID.GRASS || testId == BlockID.DIRT) {
                treeBase.setTypeIdAndData(block.getTypeId(), block.getData().getData(), true);
            }
        }
        */
    }

    private boolean isTree(BlockState block) {

        if (block.getTypeId() == BlockID.LOG) {

            Block testBlock = block.getBlock();
            do {
                testBlock = testBlock.getRelative(BlockFace.UP);
                if (testBlock.getTypeId() == BlockID.LEAVES) return true;

            } while (testBlock.getY() < block.getWorld().getHighestBlockYAt(block.getLocation()));
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {

        for (Block block : event.blockList()) {

            if (block.getWorld().getName().contains(config.wildernessWorld) && isEffectedOre(block.getTypeId())) {

                addPool(new BaseBlock(block.getTypeId()), event.getLocation());
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.wildernessWorld) && isEffectedOre(event.getBlock().getTypeId
                ())) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "You find yourself unable to place that ore.");
        }
    }

    private void addPool(final BaseBlock block, final Location altLoc) {

        TaskPool aNewTaskPool = new TaskPool(altLoc, ChanceUtil.getRangedRandom(10, 26));

        if (taskPool.containsKey(altLoc)) {
            taskPool.get(altLoc).addAmount(aNewTaskPool.amount);
            return;
        }

        BukkitTask task = server.getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                int amount = 0;
                if (taskPool.containsKey(altLoc)) {
                    amount = taskPool.get(altLoc).subAmount(1);
                }

                if (amount > 0) {
                    altLoc.getWorld().dropItemNaturally(altLoc, new ItemStack(block.getType()));
                } else {
                    altLoc.getWorld().dropItemNaturally(altLoc, new ItemStack(block.getType()));

                    // Shut down
                    if (taskPool.containsKey(altLoc)) {
                        taskPool.get(altLoc).getBukkitTask().cancel();
                        taskPool.remove(altLoc);
                    }
                }
            }
        }, 20, 20);
        aNewTaskPool.setBukkitTask(task);
        taskPool.put(altLoc, aNewTaskPool);
    }

    private static final int[] ores = {
            BlockID.GOLD_ORE
    };

    private boolean isEffectedOre(int typeId) {

        for (int ore : ores) {
            if (ore == typeId) return true;
        }
        return false;
    }

    private class TaskPool {

        private final Location location;
        private BukkitTask bukkitTask = null;
        private int amount;

        public TaskPool(Location location, int amount) {

            this.location = location;
            this.amount = amount;
        }

        public BukkitTask getBukkitTask() {

            return bukkitTask;
        }

        public void setBukkitTask(BukkitTask task) {

            this.bukkitTask = task;
        }

        public Location getLocation() {

            return location;
        }

        public void setAmount(int amount) {

            this.amount = amount;
        }

        public int addAmount(int amount) {

            this.amount += amount;
            return this.amount;
        }

        public int subAmount(int amount) {

            this.amount -= amount;
            return this.amount;
        }
    }
}