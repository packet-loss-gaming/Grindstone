/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.DeathUtil;
import org.bukkit.entity.Player;

public class DeadlyPotionEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        DeathUtil.throwSlashPotion(player.getLocation());
    }

    @Override
    public void strip(Player player) { }
}
