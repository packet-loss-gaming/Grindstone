package com.skelril.aurora.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Comparator;

/**
 * Created by wyatt on 12/15/13.
 */
public class EntityDistanceComparator implements Comparator<Entity> {

    final Location targetLoc;

    public EntityDistanceComparator(Location targetLoc) {

        this.targetLoc = targetLoc;
    }

    @Override
    public int compare(Entity o1, Entity o2) {

        return (int) (o1.getLocation().distanceSquared(targetLoc) - o2.getLocation().distanceSquared(targetLoc));
    }
}
