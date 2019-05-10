/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.checker;

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
    public Boolean evaluate(Vector vector) {
        return get().contains(vector);
    }
}
