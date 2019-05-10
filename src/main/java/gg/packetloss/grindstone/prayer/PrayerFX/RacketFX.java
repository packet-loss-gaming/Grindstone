/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class RacketFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.RACKET;
    }

    @Override
    public void add(Player player) {

        player.playSound(player.getLocation(), Sound.ZOMBIE_WOODBREAK, 1, 0);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
