/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import org.bukkit.entity.Player;

public interface PassivePrayerEffect extends PrayerEffect {
    public void trigger(Player player);
    public void strip(Player player);

    @Override
    default public PrayerEffectTrigger getTrigger() {
        return PrayerEffectTrigger.PASSIVE;
    }
}
