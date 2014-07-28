/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items;

import com.sk89q.commandbook.session.PersistentSession;
import com.skelril.aurora.items.specialattack.SpecType;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CustomItemSession extends PersistentSession {

    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(3);

    private HashMap<SpecType, Long> specMap = new HashMap<>();
    private LinkedList<Location> recentDeathLocations = new LinkedList<>();

    protected CustomItemSession() {
        super(MAX_AGE);
    }

    public void updateSpec(SpecType type) {

        specMap.put(type, System.currentTimeMillis());
    }

    public void updateSpec(SpecType type, long additionalDelay) {

        specMap.put(type, System.currentTimeMillis() + additionalDelay);
    }

    public boolean canSpec(SpecType type) {

        return !specMap.containsKey(type) || System.currentTimeMillis() - specMap.get(type) >= type.getDelay();
    }

    public void addDeathPoint(Location deathPoint) {

        recentDeathLocations.add(0, deathPoint.clone());
        while (recentDeathLocations.size() > 5) {
            recentDeathLocations.pollLast();
        }
    }

    public Location getRecentDeathPoint() {

        return recentDeathLocations.poll();
    }
}