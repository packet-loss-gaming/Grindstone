/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.TemplateComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Array;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@TemplateComponent
public abstract class AreaComponent<Config extends ConfigurationBase> extends BukkitComponent implements Runnable {
    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = inst.getLogger();
    protected final Server server = CommandBook.server();

    protected int tick;
    protected AreaListener listener;

    protected World world;
    protected ProtectedRegion region;
    protected Config config;
    protected boolean empty = true;

    public abstract void setUp();

    @Override
    public void enable() {
        setUp();
        if (listener != null) {
            //noinspection AccessStaticViaInstance
            inst.registerEvents(listener);
        }
        if (config != null) {
            config = configure(config);
            saveConfig();
        }
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, tick);
    }

    @Override
    public void reload() {
        super.reload();
        if (config != null) {
            configure(config);
        }
    }

    @Override
    public abstract void run();

    public Config getConfig() {
        return config;
    }

    public <T extends Entity> T[] getContained(Class<T> clazz) {
        return getContained(0, clazz);
    }

    public <T extends Entity> T[] getContained(int parentsUp, Class<T> clazz) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return getContained(r, clazz);
    }

    public <T extends Entity> T[] getContained(ProtectedRegion region, Class<T> clazz) {
        List<T> returnedList = world.getEntitiesByClass(clazz).stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e))
                .collect(Collectors.toList());

        //noinspection unchecked
        return returnedList.toArray((T[]) Array.newInstance(clazz, returnedList.size()));
    }

    public Entity[] getContained() {
        return getContained(Entity.class);
    }

    public Entity[] getContained(int parentsUp) {
        return getContained(parentsUp, Entity.class);
    }

    public Entity[] getContained(Class<?>... classes) {
        return getContained(0, classes);
    }

    public Entity[] getContained(int parentsUp, Class<?>... classes) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return getContained(r, classes);
    }

    public Entity[] getContained(ProtectedRegion region, Class<?>... classes) {
        List<Entity> returnedList = world.getEntitiesByClasses(classes).stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e))
                .collect(Collectors.toList());
        return returnedList.toArray(new Entity[returnedList.size()]);
    }

    public boolean isEmpty() {
        for (Player player : server.getOnlinePlayers()) {
            if (contains(player)) {
                empty = false;
                return false;
            }
        }
        empty = true;
        return true;
    }

    public boolean cachedEmpty() {
        return empty;
    }

    public boolean contains(Entity entity) {
        return contains(entity.getLocation());
    }

    public boolean contains(Entity entity, int parentsUp) {
        return contains(entity.getLocation(), parentsUp);
    }

    public boolean contains(Block block) {
        return contains(block.getLocation());
    }

    public boolean contains(Block block, int parentsUp) {
        return contains(block.getLocation(), parentsUp);
    }

    public boolean contains(Location location) {
        return LocationUtil.isInRegion(world, region, location);
    }

    public boolean contains(Location location, int parentsUp) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return LocationUtil.isInRegion(world, r, location);
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public World getWorld() {
        return world;
    }

    public File getWorkingDir() {
        return new File(inst.getDataFolder() + "/area/" + region.getId() + "/");
    }
}
