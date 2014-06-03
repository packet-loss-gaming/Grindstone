/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.city.engine.arena.AbstractRegionedArena;
import com.skelril.aurora.util.CollectionUtil;
import org.bukkit.World;

public class AbstractFactoryArea extends AbstractRegionedArena {

    protected ProtectedRegion potionChamber;
    protected ProtectedRegion[] smeltingChamber;

    public AbstractFactoryArea(World world, ProtectedRegion region,
                               ProtectedRegion potionChamber, ProtectedRegion[] smeltingChamer) {
        super(world, region);
        this.potionChamber = potionChamber;
        this.smeltingChamber = smeltingChamer;
    }

    public ProtectedRegion getChamber(ChamberType type) {
        switch (type) {
            case POTION:
                return potionChamber;
            case SMELTING:
                return CollectionUtil.getElement(smeltingChamber);
        }
        return null;
    }

    protected enum ChamberType {
        POTION,
        SMELTING
    }
}
