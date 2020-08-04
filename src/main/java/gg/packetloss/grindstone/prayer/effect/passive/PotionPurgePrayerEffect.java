/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PotionPurgePrayerEffect implements PassivePrayerEffect {
    private final PotionEffectType purgedEffectType;

    public PotionPurgePrayerEffect(PotionEffectType purgedEffectType) {
        this.purgedEffectType = purgedEffectType;
    }


    @Override
    public void trigger(Player player) {
        player.removePotionEffect(purgedEffectType);
    }

    @Override
    public void strip(Player player) { }
}
