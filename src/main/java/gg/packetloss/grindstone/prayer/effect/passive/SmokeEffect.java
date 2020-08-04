/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SmokeEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        Location[] smoke = new Location[2];
        smoke[0] = player.getLocation();
        smoke[1] = player.getEyeLocation();
        EnvironmentUtil.generateRadialEffect(smoke, Effect.SMOKE);
    }

    @Override
    public void strip(Player player) { }
}
