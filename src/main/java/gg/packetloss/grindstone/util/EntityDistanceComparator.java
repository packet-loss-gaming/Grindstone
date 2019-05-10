/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Comparator;

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
