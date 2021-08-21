/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.particle;

import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;

public class SingleBlockParticleEffect {
    public static void puffOfSmoke(Location loc) {
        for (int i = 0; i < 10; ++i) {
            loc.getWorld().spawnParticle(
                Particle.SMOKE_NORMAL,
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                0,
                ChanceUtil.getRangedRandom(-.07, .07),
                .1,
                ChanceUtil.getRangedRandom(-.07, .07)
            );
        }
    }

    public static void randomStar(Location loc) {
        loc.getWorld().spawnParticle(
            Particle.VILLAGER_HAPPY,
            loc.getX() + ChanceUtil.getRangedRandom(0D, 1D),
            loc.getY() + ChanceUtil.getRangedRandom(0D, 1D),
            loc.getZ() + ChanceUtil.getRangedRandom(0D, 1D),
            0,
            0,
            0,
            0
        );
    }

    public static void burstOfStars(Location loc) {
        for (int i = 0; i < 25; ++i) {
            randomStar(loc);
        }
    }

    public static void burstOfFlames(Location loc) {
        for (int i = 0; i < 4; i++) {
            loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
        }
    }
}
