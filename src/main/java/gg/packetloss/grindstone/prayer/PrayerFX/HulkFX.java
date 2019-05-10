/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HulkFX extends AbstractEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
            new InfiniteHungerFX()
    };
    private static PotionEffect[] effects = new PotionEffect[]{
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 4),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 30, 4)
    };

    public HulkFX() {

        super(subFX, effects);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.HULK;
    }

    @Override
    public void clean(Player player) {

        super.clean(player);
    }
}