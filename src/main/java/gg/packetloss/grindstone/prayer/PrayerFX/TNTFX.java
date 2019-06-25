/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TNTFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.TNT;
    }

    @Override
    public void add(Player player) {

        Location playerLoc = player.getLocation();
        ExplosionStateFactory.createExplosion(playerLoc, .1F, false, false);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
