/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.DeathUtil;
import org.bukkit.entity.Player;

public class DeadlyPotionFX extends AbstractEffect {
    @Override
    public PrayerType getType() {
        return PrayerType.DEADLYPOTION;
    }

    @Override
    public void add(Player player) {
        DeathUtil.throwSlashPotion(player.getLocation());
    }
}
