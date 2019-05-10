/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InvisibilityFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 600, 1);

    public InvisibilityFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.INVISIBILITY;
    }
}
