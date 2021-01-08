/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items;

import com.sk89q.commandbook.component.session.PersistentSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CustomItemSession extends PersistentSession {

    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(3);

    private HashMap<SpecType, Long> specMap = new HashMap<>();
    @Setting("death-locations")
    private LinkedList<NameBasedLocation> recentDeathLocations = new LinkedList<>();
    @Setting("grave-locations")
    private LinkedList<NameBasedLocation> recentGraves = new LinkedList<>();

    protected CustomItemSession() {
        super(MAX_AGE);
    }

    public void updateSpec(SpecType type, long delay) {
        specMap.put(type, System.currentTimeMillis() + delay);
    }

    public boolean canSpec(SpecType type) {
        return !specMap.containsKey(type) || System.currentTimeMillis() - specMap.get(type) >= 0;
    }

    private Optional<Location> popLocation(LinkedList<NameBasedLocation> locations) {
        NameBasedLocation location = locations.pollFirst();
        if (location == null) {
            return Optional.empty();
        }

        if (location.isExpired()) {
            // Clear as all older locations would be expired
            locations.clear();
            return Optional.empty();
        }

        World world = Bukkit.getWorld(location.world);
        if (world == null) {
            return popLocation(locations);
        }

        return Optional.of(WorldEditBridge.toLocation(world, location.getAsBlockVector()));
    }

    private void saveLocation(LinkedList<NameBasedLocation> locations, NameBasedLocation newLoc) {
        NameBasedLocation prevLoc = locations.peek();
        if (prevLoc != null &&
            prevLoc.world.equals(newLoc.world) &&
            LocationUtil.distanceSquared2D(newLoc.getAsBlockVector(), prevLoc.getAsBlockVector()) < Math.pow(5, 2)) {
            return;
        }

        locations.add(0, newLoc);
        while (locations.size() > 5 || (!locations.isEmpty() && locations.getLast().isExpired())) {
            locations.pollLast();
        }
    }

    private void saveLocation(LinkedList<NameBasedLocation> locations, Location newLoc) {
        saveLocation(locations, new NameBasedLocation(newLoc));
    }

    public void addDeathPoint(Location deathPoint) {
        saveLocation(recentDeathLocations, deathPoint);
    }

    public Optional<Location> getRecentDeathPoint() {
        return popLocation(recentDeathLocations);
    }

    public void addGraveLocation(Location graveLocation) {
        recentGraves.add(new NameBasedLocation(graveLocation));
    }

    public Optional<Location> getRecentGrave() {
        return popLocation(recentGraves);
    }

    public static class NameBasedLocation {
        public long creationDate;
        public String world;
        public double x;
        public double y;
        public double z;

        public NameBasedLocation(Location location) {
            this.creationDate = System.currentTimeMillis();
            this.world = location.getWorld().getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - creationDate >= MAX_AGE;
        }

        public BlockVector3 getAsBlockVector() {
            return BlockVector3.at(x, y, z);
        }
    };
}