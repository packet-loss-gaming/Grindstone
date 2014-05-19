/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.checker;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionChecker extends Checker<Region, Vector> {

    public RegionChecker(Region region) {
        super(region);
    }

    public RegionChecker(ProtectedRegion region) {
        super(new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint()));
    }

    @Override
    public boolean check(Vector vector) {
        return get().contains(vector);
    }
}
