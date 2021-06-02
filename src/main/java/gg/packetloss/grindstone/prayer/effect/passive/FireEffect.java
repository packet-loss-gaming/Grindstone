/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;

public class FireEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        if (player.getFireTicks() < 20) {
            ChatUtil.sendWarning(player, "BURN!!!");
            player.setFireTicks((20 * 60));
        }
    }

    @Override
    public void strip(Player player) { }
}
