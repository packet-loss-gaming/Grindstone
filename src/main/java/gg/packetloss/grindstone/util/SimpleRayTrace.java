package gg.packetloss.grindstone.util;

import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class SimpleRayTrace {
    private Location curLoc;
    private Vector direction;
    private int distanceRemaining;

    public SimpleRayTrace(Location startingLoc, Vector direction, int maxDistance) {
        this.curLoc = startingLoc;
        this.direction = direction.normalize();
        this.distanceRemaining = maxDistance;
    }

    public boolean hasNext() {
        return distanceRemaining > 0;
    }

    public Location next() {
        Validate.isTrue(hasNext());

        curLoc = curLoc.add(direction);
        --distanceRemaining;

        return curLoc.clone();
    }
}
