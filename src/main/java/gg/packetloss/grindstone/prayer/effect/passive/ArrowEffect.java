/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;

public class ArrowEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        Location eyeLoc = player.getEyeLocation();

        eyeLoc.setX(eyeLoc.getX());
        eyeLoc.setY(eyeLoc.getY());
        eyeLoc.setZ(eyeLoc.getZ());
        player.getWorld().spawn(eyeLoc, Arrow.class);
    }

    @Override
    public void strip(Player player) { }
}
