/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlashFX extends AbstractEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
            new InfiniteHungerFX()
    };
    private static final PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 20 * 600, 6);

    public FlashFX() {

        super(subFX, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.FLASH;
    }
}
