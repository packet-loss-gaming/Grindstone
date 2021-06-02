/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class VectorUtil {
    public static Vector createDirectionalVector(Vector from, Vector to) {
        Vector newVec = to.clone().subtract(from);
        newVec.normalize();
        return newVec;
    }

    public static Vector createDirectionalVector(Location from, Location to) {
        Vector fromVec = from.toVector();
        Vector toVec = to.toVector();

        return createDirectionalVector(fromVec, toVec);
    }
}
