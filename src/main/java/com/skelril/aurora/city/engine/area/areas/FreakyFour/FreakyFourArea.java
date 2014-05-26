/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.FreakyFour;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.area.AreaComponent;
import com.skelril.aurora.city.engine.area.PersistentArena;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.util.APIUtil;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

@ComponentInformation(friendlyName = "Freaky Four", desc = "The craziest bosses ever")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class FreakyFourArea extends AreaComponent<FreakyFourConfig> implements PersistentArena {

    protected static final int groundLevel = 79;

    @InjectComponent
    protected AdminComponent admin;

    protected Economy economy;

    protected ProtectedRegion charlotte_RG, magmacubed_RG, dabomb_RG, snipee_RG, heads;

    protected Spider charlotte;
    protected Set<MagmaCube> magmaCubed = new HashSet<>();
    protected Creeper daBomb;
    protected Skeleton snipee;

    protected Location entrance;
    protected HashMap<String, PlayerState> playerState = new HashMap<>();

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            entrance = new Location(world, 401.5, 79, -304, 270, 0);
            RegionManager manager = WG.getRegionManager(world);
            String base = "oblitus-district-freaky-four";
            region = manager.getRegion(base);
            charlotte_RG = manager.getRegion(base + "-charlotte");
            magmacubed_RG = manager.getRegion(base + "-magma-cubed");
            dabomb_RG = manager.getRegion(base + "-da-bomb");
            snipee_RG = manager.getRegion(base + "-snipee");
            heads = manager.getRegion(base + "-heads");
            tick = 4 * 20;
            listener = new FreakyFourListener(this);
            config = new FreakyFourConfig();

            reloadData();
            setupEconomy();
        } catch (UnknownPluginException e) {
            log.info("WorldGuard could not be found!");
        }
    }

    @Override
    public void enable() {
        server.getScheduler().runTaskLater(inst, super::enable, 1);
    }

    @Override
    public void disable() {
        writeData(false);
    }

    @Override
    public void run() {
        if (!isEmpty()) {
            fakeXPGain();
            if (!checkCharlotte()) {
                runCharlotte();
            }
        }
        writeData(true);
    }

    public void addSkull(Player player) {
        final BlockVector min = heads.getMinimumPoint();
        final BlockVector max = heads.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();
        com.sk89q.worldedit.Vector v = LocationUtil.pickLocation(minX, maxX, minY, maxY, minZ, maxZ);
        BukkitWorld world = new BukkitWorld(this.world);
        EditSession skullEditor = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, 1);
        skullEditor.rawSetBlock(v, new SkullBlock(12, (byte) 12, player.getName()));
    }

    protected Location getCentralLoc(ProtectedRegion region) {
        BlockVector min = region.getMinimumPoint();
        BlockVector max = region.getMaximumPoint();

        Region rg = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(world, rg.getCenter().setY(groundLevel));
    }

    public void fakeXPGain() {
        for (Player player : getContained(Player.class)) {
            if (!ItemUtil.hasNecrosArmour(player)) continue;
            for (int i = ChanceUtil.getRandom(5); i > 0; --i) {
                server.getPluginManager().callEvent(new PlayerExpChangeEvent(player,
                        ChanceUtil.getRandom(config.fakeXP)));
            }
        }
    }

    public void spawnCharlotte() {
        charlotte = getWorld().spawn(getCentralLoc(charlotte_RG), Spider.class);

        // Handle vitals
        charlotte.setMaxHealth(config.charlotteHP);
        charlotte.setHealth(config.charlotteHP);
        charlotte.setRemoveWhenFarAway(true);

        // Handle name
        charlotte.setCustomName("Charlotte");
    }

    public boolean checkCharlotte() {
        return !LocationUtil.containsPlayer(world, charlotte_RG);
    }

    public void cleanupCharlotte() {
        final BlockVector min = charlotte_RG.getMinimumPoint();
        final BlockVector max = charlotte_RG.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.WEB) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        for (CaveSpider spider : getContained(charlotte_RG, CaveSpider.class)) {
            spider.remove();
        }
    }

    public void runCharlotte() {
        if (charlotte == null) return;
        for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
            world.spawn(charlotte.getLocation(), CaveSpider.class);
        }

        final BlockVector min = charlotte_RG.getMinimumPoint();
        final BlockVector max = charlotte_RG.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        switch (ChanceUtil.getRandom(3)) {
            case 1:
                int intialTimes = maxZ - minZ + 1;
                IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {
                        int startZ = minZ + (intialTimes - times) - 1;
                        for (int y = minY; y <= maxY; ++y) {
                            for (int x = minX; x <= maxX; ++x) {
                                for (int z = startZ; z < startZ + 4; ++z) {
                                    Block block = world.getBlockAt(x, y, z);
                                    if (z == startZ && block.getType() == Material.WEB) {
                                        block.setType(Material.AIR);
                                    } else if (block.getType() == Material.AIR) {
                                        block.setType(Material.WEB);
                                    }
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    public void end() {
                        for (int x = minX; x <= maxX; ++x) {
                            for (int z = minZ; z <= maxZ; ++z) {
                                if (!ChanceUtil.getChance(config.charlotteFloorWeb)) continue;
                                Block block = world.getBlockAt(x, groundLevel, z);
                                if (block.getType() == Material.AIR) {
                                    block.setType(Material.WEB);
                                }
                            }
                        }
                    }
                };
                TimedRunnable timedRunnable = new TimedRunnable(integratedRunnable, intialTimes);
                BukkitTask task = server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 5);
                timedRunnable.setTask(task);
                break;
            case 2:
                LivingEntity target = charlotte.getTarget();
                if (target != null) {
                    List<Location> queList = new ArrayList<>();
                    for (Location loc : Arrays.asList(target.getLocation(), target.getEyeLocation())) {
                        for (BlockFace face : EnvironmentUtil.getNearbyBlockFaces()) {
                            if (face == BlockFace.SELF) continue;
                            queList.add(loc.getBlock().getRelative(face).getLocation());
                        }
                    }
                    for (Location loc : queList) {
                        Block block = world.getBlockAt(loc);
                        if (block.getType().isSolid()) continue;
                        block.setType(Material.WEB);
                    }
                }
                break;
            case 3:
                for (int y = minY; y <= maxY; ++y) {
                    for (int x = minX; x <= maxX; ++x) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            if (!ChanceUtil.getChance(config.charlotteWebSpider)) continue;
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() == Material.WEB) {
                                block.setType(Material.AIR);
                                world.spawn(block.getLocation(), CaveSpider.class);
                            }
                        }
                    }
                }
                break;
        }
    }

    public void spawnMagmaCubed() {
        final MagmaCube cube = getWorld().spawn(getCentralLoc(magmacubed_RG), MagmaCube.class);

        // Handle vitals
        cube.setRemoveWhenFarAway(true);
        cube.setSize(config.magmaCubedSize);
        // Work around for health
        server.getScheduler().runTaskLater(inst, () -> {
            cube.setMaxHealth(config.magmaCubedHP);
            cube.setHealth(config.magmaCubedHP);
        }, 1);

        // Handle name
        cube.setCustomName("Magma Cubed");
        magmaCubed.add(cube);
    }

    public boolean checkMagmaCubed() {
        return !LocationUtil.containsPlayer(world, magmacubed_RG);
    }

    public void spawnDaBomb() {
        daBomb = getWorld().spawn(getCentralLoc(dabomb_RG), Creeper.class);

        // Handle vitals
        daBomb.setMaxHealth(config.daBombHP);
        daBomb.setHealth(config.daBombHP);
        daBomb.setRemoveWhenFarAway(true);

        // Handle name
        daBomb.setCustomName("Da Bomb");
    }

    public boolean checkDaBomb() {
        return !LocationUtil.containsPlayer(world, dabomb_RG);
    }

    public void spawnSnipee() {
        snipee = getWorld().spawn(getCentralLoc(snipee_RG), Skeleton.class);

        // Handle vitals
        snipee.setMaxHealth(config.snipeeHP);
        snipee.setHealth(config.snipeeHP);
        snipee.setRemoveWhenFarAway(true);

        // Handle name
        snipee.setCustomName("Snipee");
    }

    public boolean checkSnipee() {
        return !LocationUtil.containsPlayer(world, snipee_RG);
    }

    // Cleans up empty arenas
    public void validateBosses() {
        if (charlotte != null) {
            if (checkCharlotte()) {
                if (charlotte.isValid()) {
                    charlotte.remove();
                }
                charlotte = null;
            } else if (!charlotte.isValid()) {
                charlotte = null;
            }
        }
        if (!magmaCubed.isEmpty()) {
            boolean checkMagmaCubed = checkMagmaCubed();
            Iterator<MagmaCube> it = magmaCubed.iterator();
            while (it.hasNext()) {
                MagmaCube cube = it.next();
                if (checkMagmaCubed) {
                    if (cube.isValid()) {
                        cube.remove();
                    }
                    it.remove();
                } else if (!cube.isValid()) {
                    it.remove();
                }
            }
        }
        if (daBomb != null) {
            if (checkDaBomb()) {
                if (daBomb.isValid()) {
                    daBomb.remove();
                }
                daBomb = null;
            } else if (!daBomb.isValid()) {
                daBomb = null;
            }
        }
        if (snipee != null) {
            if (checkSnipee()) {
                if (snipee.isValid()) {
                    snipee.remove();
                }
                snipee = null;
            } else if (!snipee.isValid()) {
                snipee = null;
            }
        }
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    @Override
    public void writeData(boolean doAsync) {
        Runnable run = () -> {
            respawnsFile:
            {
                File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
                if (playerStateFile.exists()) {
                    Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);

                    if (playerState.equals(playerStateFileO)) {
                        break respawnsFile;
                    }
                }
                IOUtil.toBinaryFile(getWorkingDir(), "respawns", playerState);
            }
        };
        if (doAsync) {
            server.getScheduler().runTaskAsynchronously(inst, run);
        } else {
            run.run();
        }
    }

    @Override
    public void reloadData() {
        File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
        if (playerStateFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
            if (playerStateFileO instanceof HashMap) {
                //noinspection unchecked
                playerState = (HashMap<String, PlayerState>) playerStateFileO;
                log.info("Loaded: " + playerState.size() + " respawn records for the Freaky Four.");
            } else {
                log.warning("Invalid block record file encountered: " + playerStateFile.getName() + "!");
                log.warning("Attempting to use backup file...");
                playerStateFile = new File(getWorkingDir().getPath() + "/old-" + playerStateFile.getName());
                if (playerStateFile.exists()) {
                    playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
                    if (playerStateFileO instanceof HashMap) {
                        //noinspection unchecked
                        playerState = (HashMap<String, PlayerState>) playerStateFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + playerState.size() + " respawn records for the Freaky Four.");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
    }
}
