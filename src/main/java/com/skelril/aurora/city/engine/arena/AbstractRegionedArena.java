/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Author: Turtle9598
 */
public abstract class AbstractRegionedArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private boolean empty = true;
    private World world;
    private ProtectedRegion region;

    public AbstractRegionedArena(World world, ProtectedRegion region) {

        this.world = world;
        this.region = region;
    }


    public <T extends Entity> Collection<T> getContained(Class<T> clazz) {
        return getContained(0, clazz);
    }

    public <T extends Entity> Collection<T> getContained(int parentsUp, Class<T> clazz) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return getContained(r, clazz);
    }

    public <T extends Entity> Collection<T> getContained(ProtectedRegion region, Class<T> clazz) {
        return world.getEntitiesByClass(clazz).stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e))
                .collect(Collectors.toList());
    }

    public Collection<Entity> getContained() {
        return getContained(Entity.class);
    }

    public Collection<Entity> getContained(int parentsUp) {
        return getContained(parentsUp, Entity.class);
    }

    public Collection<Entity> getContained(Class<?>... classes) {
        return getContained(0, classes);
    }

    public Collection<Entity> getContained(int parentsUp, Class<?>... classes) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return getContained(r, classes);
    }

    public Collection<Entity> getContained(ProtectedRegion region, Class<?>... classes) {
        return world.getEntitiesByClasses(classes).stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e))
                .collect(Collectors.toList());
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
