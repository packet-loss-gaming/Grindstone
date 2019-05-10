/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public abstract class AbstractTriggeredEffect extends AbstractEffect {

    private final Class triggerClass;

    public AbstractTriggeredEffect(Class triggerClass) {

        this.triggerClass = triggerClass;
    }

    public AbstractTriggeredEffect(Class triggerClass, AbstractEffect[] subFX) {

        super(subFX);
        this.triggerClass = triggerClass;
    }

    public AbstractTriggeredEffect(Class triggerClass, AbstractEffect[] subFX, PotionEffect... effects) {

        super(subFX, effects);
        this.triggerClass = triggerClass;
    }

    public Class getTriggerClass() {

        return triggerClass;
    }

    public abstract void trigger(Player player);

}
