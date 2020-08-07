/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import com.google.common.collect.ImmutableList;

import java.util.Map;

public class Prayer {
    private final boolean isHoly;
    private final Map<PrayerEffectTrigger, ImmutableList<PrayerEffect>> effects;
    private final long startTime;
    private long maxDuration;

    protected Prayer(boolean isHoly, Map<PrayerEffectTrigger, ImmutableList<PrayerEffect>> effects, long startTime, long maxDuration) {
        this.isHoly = isHoly;
        this.effects = effects;
        this.startTime = startTime;
        this.maxDuration = maxDuration;
    }

    protected Prayer(boolean isHoly, Map<PrayerEffectTrigger, ImmutableList<PrayerEffect>> effects, long maxDuration) {
        this(isHoly, effects, System.currentTimeMillis(), maxDuration);
    }

    protected Prayer(Prayers prayer, long maxDuration) {
        this(prayer.isHoly(), prayer.getEffects(), System.currentTimeMillis(), maxDuration);
    }

    public boolean isHoly() {
        return isHoly;
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

    public boolean hasExpired() {
        return System.currentTimeMillis() - getStartTime() > getMaxDuration();
    }

    public ImmutableList<PassivePrayerEffect> getPassiveEffects() {
        return (ImmutableList) effects.get(PrayerEffectTrigger.PASSIVE);
    }

    public ImmutableList<InteractTriggeredPrayerEffect> getInteractiveEffects() {
        return (ImmutableList) effects.get(PrayerEffectTrigger.INTERACT);
    }
}
