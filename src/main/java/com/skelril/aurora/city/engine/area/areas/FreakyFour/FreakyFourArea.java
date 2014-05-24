/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.FreakyFour;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
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
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import org.bukkit.Location;
import org.bukkit.entity.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@ComponentInformation(friendlyName = "Freaky Four", desc = "The craziest bosses ever")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class FreakyFourArea extends AreaComponent<FreakyFourConfig> implements PersistentArena {

    protected static final int groundLevel = 79;

    @InjectComponent
    protected AdminComponent admin;

    protected ProtectedRegion charlotte_RG, magmacubed_RG, dabomb_RG, snipee_RG;

    protected Spider charolette;
    protected Set<MagmaCube> magmaCubed = new HashSet<>();
    protected Creeper daBomb;
    protected Skeleton snipee;

    protected HashMap<String, PlayerState> playerState = new HashMap<>();

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            RegionManager manager = WG.getRegionManager(world);
            String base = "oblitus-district-freaky-four";
            region = manager.getRegion(base);
            charlotte_RG = manager.getRegion(base + "-charlotte");
            magmacubed_RG = manager.getRegion(base + "-magma-cubed");
            dabomb_RG = manager.getRegion(base + "-da-bomb");
            snipee_RG = manager.getRegion(base + "-snipee");
            tick = 4 * 20;
            listener = new FreakyFourListener(this);
            config = new FreakyFourConfig();

            reloadData();
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
        if (!checkCharlotte())  {
            runCharlotte();
        }
        writeData(true);
    }

    protected Location getCentralLoc(ProtectedRegion region) {
        BlockVector min = region.getMinimumPoint();
        BlockVector max = region.getMaximumPoint();

        Region rg = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(world, rg.getCenter().setY(groundLevel));
    }

    public void spawnCharlotte() {
        charolette = getWorld().spawn(getCentralLoc(charlotte_RG), Spider.class);

        // Handle vitals
        charolette.setMaxHealth(config.charlotteHP);
        charolette.setHealth(config.charlotteHP);
        charolette.setRemoveWhenFarAway(true);

        // Handle name
        charolette.setCustomName("Charlotte");
    }

    public boolean checkCharlotte() {
        return !LocationUtil.containsPlayer(world, charlotte_RG);
    }

    public void runCharlotte() {
        if (charolette == null) return;
        Location target = getCentralLoc(charlotte_RG);
        for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
            world.spawn(target, CaveSpider.class);
        }
        // TODO Add web attacks
    }

    public void spawnMagmaCubed() {
        MagmaCube cube = getWorld().spawn(getCentralLoc(magmacubed_RG), MagmaCube.class);

        // Handle vitals
        cube.setMaxHealth(config.magmaCubedHP);
        cube.setHealth(config.magmaCubedHP);
        cube.setRemoveWhenFarAway(true);
        cube.setSize(config.magmaCubedSize);

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
        if (charolette != null) {
            if (checkCharlotte()) {
                if (charolette.isValid()) {
                    charolette.remove();
                }
                charolette = null;
            } else if (!charolette.isValid()) {
                charolette = null;
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
