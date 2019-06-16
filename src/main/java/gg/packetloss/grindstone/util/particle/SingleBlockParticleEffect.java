/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.particle;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class SingleBlockParticleEffect {
    public static void puffOfSmoke(Location loc) {
        loc.getWorld().playEffect(loc, Effect.SMOKE, BlockFace.UP);
    }

    public static void burstOfFlames(Location loc) {
        for (int i = 0; i < 4; i++) {
            loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
        }
    }
}
