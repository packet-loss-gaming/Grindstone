/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WalkFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 20 * 600, 2);

    public WalkFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.WALK;
    }
}
