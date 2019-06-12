/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import gg.packetloss.grindstone.prayer.PrayerFX.AbstractEffect;
import gg.packetloss.grindstone.prayer.PrayerFX.AbstractTriggeredEffect;
import org.bukkit.entity.Player;

public class Prayer implements Comparable<Prayer> {

    private final Player player;
    private final AbstractEffect abstractEffect;
    private final long startTime;
    private long maxDuration;


    protected Prayer(Player player, AbstractEffect abstractEffect, long maxDuration) {

        this.player = player;
        this.abstractEffect = abstractEffect;
        this.startTime = System.currentTimeMillis();
        this.maxDuration = maxDuration;
    }

    public Player getPlayer() {

        return player;
    }

    public AbstractEffect getEffect() {

        return abstractEffect;
    }

    public long getStartTime() {

        return startTime;
    }

    public long getMaxDuration() {

        return maxDuration;
    }

    public void setMaxDuration(long maxDuration) {

        this.maxDuration = maxDuration;
    }

    public boolean hasTrigger() {

        return abstractEffect instanceof AbstractTriggeredEffect;
    }

    public Class getTriggerClass() {

        return hasTrigger() ? ((AbstractTriggeredEffect) abstractEffect).getTriggerClass() : null;
    }

    @Override
    public int compareTo(Prayer prayer) {

        if (prayer == null) return 0;

        return Integer.compare(this.getEffect().getType().getValue(), prayer.getEffect().getType().getValue());
    }
}
