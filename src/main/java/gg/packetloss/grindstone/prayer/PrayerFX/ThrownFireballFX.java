/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class ThrownFireballFX extends AbstractTriggeredEffect {

    private long nextTime = -1;

    public ThrownFireballFX() {

        super(PlayerInteractEvent.class);
    }

    @Override
    public void trigger(Player player) {

        if (nextTime != -1 && System.currentTimeMillis() < nextTime) return;

        Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2))
                .toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
        player.getWorld().spawn(loc, Fireball.class);

        nextTime = System.currentTimeMillis() + 750;
    }

    @Override
    public PrayerType getType() {

        return PrayerType.FIREBALL;
    }
}
