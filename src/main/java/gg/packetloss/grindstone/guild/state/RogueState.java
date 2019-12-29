package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.RoguePower;

import java.util.concurrent.TimeUnit;

public class RogueState extends InternalGuildState {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private boolean limitYVelocity = false;
    private boolean grenadeSafety = true;

    private long nextBlip = 0;
    private long nextGrenade = 0;

    public RogueState(long experience) {
        super(experience);
    }

    public boolean canBlip() {
        return nextBlip == 0 || System.currentTimeMillis() >= nextBlip;
    }

    private void blip(long time) {
        nextBlip = System.currentTimeMillis() + time;
    }

    public void blip() {
        blip(2250);
    }

    public void stallBlip() {
        blip(TimeUnit.SECONDS.toMillis(12));
    }

    public boolean canGrenade() {
        return nextGrenade == 0 || System.currentTimeMillis() >= nextGrenade;
    }

    public void grenade() {
        nextGrenade = System.currentTimeMillis() + 3500;
    }

    public boolean isYLimited() {
        return limitYVelocity;
    }

    public void limitYVelocity(boolean limitYVelocity) {
        this.limitYVelocity = limitYVelocity;
    }

    public boolean getGrenadeSafety() {
        return grenadeSafety;
    }

    public void setGrenadeSafety(boolean grenadeSafety) {
        this.grenadeSafety = grenadeSafety;
    }

    public boolean hasPower(RoguePower power) {
        return getLevel() >= power.getUnlockLevel();
    }

    @Override
    public GuildType getType() {
        return GuildType.ROGUE;
    }
}
