/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.RoguePower;

import java.util.concurrent.TimeUnit;

public class RogueState extends InternalGuildState {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private long nextBlip = 0;
    private long nextGrenade = 0;
    private int hits = 0;
    private boolean impact = false;

    private final RogueStateSettings settings;

    public RogueState(long experience, RogueStateSettings settings) {
        super(experience);
        this.settings = settings;
    }

    public boolean canBlip() {
        return nextBlip == 0 || System.currentTimeMillis() >= nextBlip;
    }

    private void blip(long time) {
        nextBlip = System.currentTimeMillis() + time;
    }

    public void blip() {
        blip(1750);
    }

    public void stallBlip() {
        blip(5250);
    }

    public boolean canGrenade() {
        return nextGrenade == 0 || System.currentTimeMillis() >= nextGrenade;
    }

    public void grenade() {
        nextGrenade = System.currentTimeMillis() + 3500;
    }

    public void hitEntity() {
        ++hits;
    }

    public void clearHits() {
        hits = 0;
    }

    public int getUninterruptedHits() {
        return hits;
    }

    public void setImpactEnabled(boolean impact) {
        this.impact = impact;
    }

    public boolean isUsingImpact() {
        return impact;
    }

    public boolean hasPower(RoguePower power) {
        return hasLevelForPower(power);
    }

    @Override
    public GuildType getType() {
        return GuildType.ROGUE;
    }

    @Override
    public RogueStateSettings getSettings() {
        return settings;
    }
}
