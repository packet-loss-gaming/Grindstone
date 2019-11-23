/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Location;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.Random;

public class DeathUtil {

    private static final Random random = new Random();

    private static final PotionType[] thrownTypes = new PotionType[]{
            PotionType.INSTANT_DAMAGE, PotionType.INSTANT_DAMAGE,
            PotionType.POISON, PotionType.WEAKNESS
    };

    public static void throwSlashPotion(Location location) {
        PotionType type = CollectionUtil.getElement(thrownTypes);

        ThrownPotion potion = SplashPotionUtil.throwMaxedSplashPotion(location, type);
        potion.setVelocity(new Vector(
                random.nextDouble() * .5 - .25,
                random.nextDouble() * .4 + .1,
                random.nextDouble() * .5 - .25
        ));
    }
}
