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

public class AbsorptionFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.ABSORPTION, 20 * 600, 4);

    public AbsorptionFX() {

        super(null, effect);
    }

    @Override
    public void clean(Player player) {

        // Don't clean this effect, it messes it up
    }

    @Override
    public void kill(Player player) {

        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.ABSORPTION;
    }
}