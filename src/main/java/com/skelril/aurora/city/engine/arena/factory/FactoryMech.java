/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.city.engine.arena.AbstractRegionedArena;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FactoryMech extends AbstractRegionedArena {

    protected Map<Integer, Integer> items = new ConcurrentHashMap<>();

    public FactoryMech(World world, ProtectedRegion region) {
        super(world, region);
    }

    public abstract List<ItemStack> process();
}
