/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Author: Turtle9598
 */
public class JailCell {

    private final String name;
    private final String prison;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public JailCell(String name, String prison, String world, int x, int y, int z) {

        this.name = name;
        this.prison = prison;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getCellName() {

        return name;
    }

    public String getPrisonName() {

        return prison;
    }

    public String getWorldName() {

        return world;
    }

    public int getX() {

        return x;
    }

    public int getY() {

        return y;
    }

    public int getZ() {

        return z;
    }

    public Location getLocation() {

        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    @Override
    public boolean equals(Object other) {

        if (!(other instanceof JailCell)) {
            return false;
        }
        JailCell jailCell = (JailCell) other;
        return potentialNullEquals(this.name, jailCell.name);
    }

    public static boolean potentialNullEquals(Object a, Object b) {

        return (a == null && b == null)
                || a != null && b != null
                && a.equals(b);
    }

    @Override
    public int hashCode() {

        int result = name != null ? name.hashCode() : 0;
        result = 32 * result;
        return result;
    }
}