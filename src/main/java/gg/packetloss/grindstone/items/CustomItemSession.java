/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items;

import com.sk89q.commandbook.component.session.PersistentSession;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CustomItemSession extends PersistentSession {

    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(3);

    private HashMap<SpecType, Long> specMap = new HashMap<>();
    private LinkedList<Location> recentDeathLocations = new LinkedList<>();
    private LinkedList<Location> recentDropLocations = new LinkedList<>();

    protected CustomItemSession() {
        super(MAX_AGE);
    }

    public void updateSpec(SpecType type, long delay) {
        specMap.put(type, System.currentTimeMillis() + delay);
    }

    public boolean canSpec(SpecType type) {
        return !specMap.containsKey(type) || System.currentTimeMillis() - specMap.get(type) >= 0;
    }

    private void saveLocation(LinkedList<Location> locations, Location newLoc) {
        Location prevLoc = locations.peek();
        if (prevLoc != null &&
                prevLoc.getWorld().equals(newLoc.getWorld()) &&
                prevLoc.distanceSquared(newLoc) < Math.pow(5, 2)) {
            return;
        }

        locations.add(0, newLoc.clone());
        while (locations.size() > 5) {
            locations.pollLast();
        }
    }

    public void addDeathPoint(Location deathPoint) {
        saveLocation(recentDeathLocations, deathPoint);
    }

    public Location getRecentDeathPoint() {
        return recentDeathLocations.poll();
    }

    public void addDeathDropLocation(Location dropPoint) {
        saveLocation(recentDropLocations, dropPoint);
    }

    public Location getRecentDeathDropPoint() {
        return recentDropLocations.poll();
    }
}