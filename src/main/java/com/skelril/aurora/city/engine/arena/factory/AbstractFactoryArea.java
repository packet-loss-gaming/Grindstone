/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.city.engine.arena.AbstractRegionedArena;
import org.bukkit.World;

public class AbstractFactoryArea extends AbstractRegionedArena {

    protected ProtectedRegion chamber;

    public AbstractFactoryArea(World world, ProtectedRegion region, ProtectedRegion chamber) {
        super(world, region);
        this.chamber = chamber;
    }

    public ProtectedRegion getChamber() {

        return chamber;
    }
}
