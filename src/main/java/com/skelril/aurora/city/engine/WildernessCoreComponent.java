/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.admin.AdminState;
import com.skelril.aurora.events.PlayerAdminModeChangeEvent;
import com.skelril.aurora.events.apocalypse.ApocalypseLocalSpawnEvent;
import com.skelril.aurora.events.entity.item.DropClearPulseEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import com.skelril.aurora.util.item.custom.CustomItems;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
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
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Wilderness Core", desc = "Operate the wilderness.")
@Depend(components = {AdminComponent.class, SessionComponent.class})
public class WildernessCoreComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private SessionComponent sessions;

    private long nextDropTime = 0;
    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20 * 2);

        registerCommands(Commands.class);
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

            if (wilderness.getTime() % 7 == 0) {
                for (Entity item : wilderness.getEntitiesByClasses(Item.class)) {

                    ItemStack stack = ((Item) item).getItemStack();

                    if (stack.getAmount() > 1) continue;

                    if (item.getTicksLived() > 20 * 60 && stack.getTypeId() == BlockID.SAPLING) {
                        item.getLocation().getBlock().setTypeIdAndData(stack.getTypeId(),
                                stack.getData().getData(), true);
                        item.remove();
                    }
                }
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

            city.getEntitiesByClasses(Horse.class).forEach(this::tryTeleport);
            wilderness.getEntitiesByClasses(Horse.class).forEach(this::tryTeleport);
        }
    }

    private void tryTeleport(final Entity vehicle) {

        final Entity passenger = vehicle == null ? null : vehicle.getPassenger();

        if (passenger != null && vehicle.getLocation().getBlock().getTypeId() == BlockID.END_PORTAL) {
            vehicle.eject();
            server.getScheduler().runTaskLater(inst, () -> {
                if (!vehicle.isValid() || !passenger.isValid()) return;
                vehicle.teleport(passenger);
                vehicle.setPassenger(passenger);

                if (passenger instanceof Player) {
                    ((Player) passenger).kickPlayer("Please Reconnect");
                }
            }, 20);
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
                } else if (from.getWorld().getName().startsWith(config.wildernessWorld)) {
                    pLoc.setWorld(wilderness);
                    pLoc.setX(pLoc.getBlockX() * 8);
                    pLoc.setZ(pLoc.getBlockZ() * 8);
                    agent.setCanCreatePortal(true);
                    event.setPortalTravelAgent(agent);
                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                }

                // City Code
                if (from.getWorld().getName().startsWith(config.cityWorld)) {
                    event.setTo(LocationUtil.grandBank(city));
                    agent.setCanCreatePortal(false);
                    event.setPortalTravelAgent(agent);
                } else if (to.getWorld().getName().startsWith(config.cityWorld)) {
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
        if (event.getWorld().getName().startsWith(config.cityWorld)) event.setCancelled(true);
    }

    @EventHandler
    public void onAdminModeChange(PlayerAdminModeChangeEvent event) {

        World world = event.getPlayer().getWorld();

        if (event.getNewAdminState().equals(AdminState.SYSOP)) return;
        if (!event.getNewAdminState().equals(AdminState.MEMBER) && world.getName().startsWith(config.wildernessWorld)) {
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

        if (to.startsWith(config.wildernessWorld) && adminComponent.isAdmin(player)) {

            adminComponent.deadmin(player);
        }
    }

    // Catch possible escapes
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().startsWith(config.wildernessWorld) && adminComponent.isAdmin(player)) {

            adminComponent.deadmin(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {

        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Monster)) return;

        Location location = event.getLocation();
        int level = getLevel(location);
        if (location.getWorld().getName().startsWith(config.wildernessWorld) && level > 1) {
            double max = entity.getMaxHealth();

            level--;

            entity.setMaxHealth(max * 5 * level);
            entity.setHealth(max * 5 * level);
        }
    }

    private static Set<EntityDamageEvent.DamageCause> ignoredDamage = new HashSet<>();

    static {
        ignoredDamage.add(EntityDamageEvent.DamageCause.WITHER);
        ignoredDamage.add(EntityDamageEvent.DamageCause.FIRE_TICK);
        ignoredDamage.add(EntityDamageEvent.DamageCause.POISON);
        ignoredDamage.add(EntityDamageEvent.DamageCause.CONTACT);
        ignoredDamage.add(EntityDamageEvent.DamageCause.DROWNING);
        ignoredDamage.add(EntityDamageEvent.DamageCause.FALL);
        ignoredDamage.add(EntityDamageEvent.DamageCause.STARVATION);
        ignoredDamage.add(EntityDamageEvent.DamageCause.SUFFOCATION);
        ignoredDamage.add(EntityDamageEvent.DamageCause.VOID);
        ignoredDamage.add(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
        ignoredDamage.add(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        final Entity entity = event.getEntity();

        Location location = entity.getLocation();
        if (!location.getWorld().getName().startsWith(config.wildernessWorld)) return;

        if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();

            if (damager instanceof Projectile && ((Projectile) damager).getShooter() != null) {
                ProjectileSource source = ((Projectile) damager).getShooter();
                if (source instanceof Entity) {
                    damager = (Entity) source;
                }
            }

            if (damager instanceof Player) {

                if (entity instanceof Player) {
                    return;
                }

                final Entity finalDamager = damager;
                final int oldCurrent = (int) Math.ceil(((LivingEntity) entity).getHealth());

                server.getScheduler().runTaskLater(inst, () -> {

                    int current = (int) Math.ceil(((LivingEntity) entity).getHealth());

                    if (oldCurrent == current) return;

                    WildernessSession session = sessions.getSession(WildernessSession.class, (CommandSender) finalDamager);

                    int max = (int) Math.ceil(((LivingEntity) entity).getMaxHealth());

                    String message;

                    if (current > 0) {
                        message = ChatColor.DARK_AQUA
                                + String.valueOf(session.checkLast(entity.getUniqueId()) ? ChatColor.ITALIC : "")
                                + "Entity Health: " + current + " / " + max;
                    } else {
                        message = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "KO!";
                    }

                    ChatUtil.sendNotice((Player) finalDamager, message);
                }, 1);
                return;
            }
        }

        if (!(entity instanceof Player) || ignoredDamage.contains(event.getCause())) return;

        int level = getLevel(location);

        if (level > 1) {

            event.setDamage(event.getDamage() * level);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {

        Entity entity = event.getEntity();

        if (!(entity instanceof Monster) || event.getDroppedExp() < 1) return;

        Location location = entity.getLocation();
        int level = getLevel(location);
        if (location.getWorld().getName().startsWith(config.wildernessWorld) && level > 1) {

            double diffLevel = Math.max(1, level * .63);
            for (int i = 0; i < diffLevel * diffLevel * diffLevel; i++) {
                if (ChanceUtil.getChance(100000)) event.getDrops().add(CustomItemCenter.build(CustomItems.RED_FEATHER));
                if (ChanceUtil.getChance(2000)) event.getDrops().add(ItemUtil.MPotion.potionOfRestitution());
            }

            event.getDrops().addAll(
                    SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 1, Math.pow(level, 2) * 64)
            );
            event.setDroppedExp(event.getDroppedExp() * level);
        }
    }

    public int getLevel(Location location) {

        // Not in Wilderness
        if (!location.getWorld().getName().startsWith(config.wildernessWorld)) {
            return 0;
        }

        // In Wilderness
        return Math.max(0, Math.max(Math.abs(location.getBlockX()), Math.abs(location.getBlockZ())) / 1000) + 1;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        final BlockState block = event.getBlock().getState();

        if (!player.getWorld().getName().startsWith(config.wildernessWorld)) return;

        if (isEffectedOre(block.getTypeId())) {

            ItemStack stack = player.getItemInHand();

            addPool(block, ItemUtil.fortuneLevel(stack), stack.containsEnchantment(Enchantment.SILK_TOUCH));
        }  /* else if (block.getTypeId() == BlockID.SAPLING) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "You cannot break that here.");
        }
        */

        event.setExpToDrop(event.getExpToDrop() * getLevel(block.getLocation()));

        /*
        if (isTree(block)) {

            Block treeBase = block.getBlock();
            while (treeBase.getTypeId() != BlockID.GRASS && treeBase.getTypeId() != BlockID.DIRT) {
                treeBase = treeBase.getRelative(BlockFace.DOWN);
                if (treeBase.getY() < 3) break;
            }

            treeBase = treeBase.getRelative(BlockFace.UP);

            treeBase.breakNaturally();
            treeBase.setTypeIdAndData(BlockID.SAPLING, block.getData().getData(), true);
        }
        */
    }

    private boolean isTree(BlockState block) {

        int worldHeightLimit = block.getWorld().getMaxHeight();
        if (block.getTypeId() == BlockID.LOG) {

            Block testBlock = block.getBlock();
            do {
                testBlock = testBlock.getRelative(BlockFace.UP);
                Location[] nearbyLocs = LocationUtil.getNearbyLocations(testBlock.getLocation(), 1);
                for (Location aLocation : nearbyLocs) {
                    if (aLocation.getBlock().getTypeId() == BlockID.LEAVES) return true;
                }
            } while (testBlock.getY() < Math.min(worldHeightLimit, testBlock.getY() + 20));
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {

        Entity entity = event.getEntity();
        if (!(entity instanceof Creeper || entity instanceof Fireball)) return;

        Location loc = entity.getLocation();
        if (!loc.getWorld().getName().startsWith(config.wildernessWorld)) return;

        float min = event.getRadius();
        event.setRadius(Math.min(entity instanceof Fireball ? 4 : 9, Math.max(min, (min + getLevel(loc)) / 2)));

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {

        if (!event.getLocation().getWorld().getName().startsWith(config.wildernessWorld)) return;

        event.setYield(.1F);

        event.blockList().stream().filter(block -> isEffectedOre(block.getTypeId()))
                .forEach(block -> addPool(block.getState(), 0, false));
    }

    @EventHandler
    public void onDropClearPulse(DropClearPulseEvent event) {

        if (!event.getWorld().getName().startsWith(config.wildernessWorld)) return;
        nextDropTime = System.currentTimeMillis() + (event.getSecondsLeft() * 1000);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLocalSpawn(ApocalypseLocalSpawnEvent event) {

        Location location = event.getLocation();

        if (!location.getWorld().getName().startsWith(config.wildernessWorld)) return;

        int level = getLevel(location);
        if (!ChanceUtil.getChance(level * level)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().startsWith(config.wildernessWorld)) {
            int typeId = event.getBlock().getTypeId();
            if (isEffectedOre(typeId)) {
                event.setCancelled(true);
                ChatUtil.sendError(player, "You find yourself unable to place that ore.");
            } /* else if (typeId == BlockID.SAPLING) {
                event.setCancelled(true);
                ChatUtil.sendError(player, "You cannot plant that here.");
            }
            */
        }
    }

    public class Commands {

        @Command(aliases = {"wlevel", "wl"},
                usage = "[target level]", desc = "Get current wilderness level",
                flags = "", min = 0, max = 1)
        public void lostPotionOfRestitutionCmd(CommandContext args, CommandSender sender) throws CommandException {

            int level;
            if (args.argsLength() > 0) {
                level = args.getInteger(0);
            } else {
                level = Math.max(1, getLevel(PlayerUtil.checkPlayer(sender).getLocation()));
            }
            ChatUtil.sendNotice(sender, "Current Level: " + (level == 0 ? ChatColor.RED + "Not available" : level)
                    + ChatColor.YELLOW + ", Target Level: " + level + ".\n");

            DecimalFormat df = new DecimalFormat("#.#");

            ChatUtil.sendNotice(sender, "Damage Modifier: " + level + "x");
            ChatUtil.sendNotice(sender, "Ore Pool Modifier: " + df.format(Math.max(1, (level * 1.2) / 3)) + "x");
            ChatUtil.sendNotice(sender, "Mob Health Modifier: " + (level > 1 ? 5 * (level - 1) : 1) + "x");
        }
    }

    private void addPool(final BlockState block, final int fortuneLevel, final boolean hasSilkTouch) {

        final Location location = block.getLocation();
        final World world = location.getWorld();

        ItemStack generalDrop = EnvironmentUtil.getOreDrop(block.getTypeId(), hasSilkTouch);
        final int fortune = EnvironmentUtil.isOre(generalDrop.getTypeId()) ? 0 : fortuneLevel;
        final int times = (int) ChanceUtil.getRangedRandom(3, Math.max(3, getLevel(location) * 1.2));
        final float vol = ((float) 1 / times);
        IntegratedRunnable dropper = new IntegratedRunnable() {
            @Override
            public boolean run(int timesL) {

                if (nextDropTime != 0 && System.currentTimeMillis() < nextDropTime) return false;

                for (int i = 0; i < ItemUtil.fortuneModifier(block.getTypeId(), fortune); i++) {
                    world.dropItem(location, EnvironmentUtil.getOreDrop(block.getTypeId(), hasSilkTouch));
                }
                world.playSound(location, Sound.BLAZE_BREATH, Math.min(1, (((float) timesL / times) * .6F) + vol), 0);
                return true;
            }

            @Override
            public void end() {

                world.playSound(location, Sound.BLAZE_DEATH, .2F, 0);
            }
        };

        TimedRunnable runnable = new TimedRunnable(dropper, times);

        BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 20, 20);

        runnable.setTask(task);
    }

    /*
    private static final int[] ores = {
            BlockID.GOLD_ORE
    };
    */

    private boolean isEffectedOre(int typeId) {

        /*
        for (int ore : ores) {
            if (ore == typeId) return true;
        }
        */
        return EnvironmentUtil.isOre(typeId);
    }

    private static class WildernessSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private UUID lastAttacked = null;

        protected WildernessSession() {
            super(MAX_AGE);
        }

        public boolean checkLast(UUID lastAttacked) {

            if (this.lastAttacked == lastAttacked) {
                return true;
            }

            this.lastAttacked = lastAttacked;

            return false;
        }
    }
}