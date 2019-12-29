package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildLevel;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.RoguePower;

import java.util.concurrent.TimeUnit;

public class RogueState extends InternalGuildState {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private long lastAttack = 0;
    private long lastLeftClick = 0;
    private long lastRightClick = 0;

    private long nextBlip = 0;
    private long nextGrenade = 0;

    public RogueState(long experience) {
        super(experience);
    }

    public void recordAttack() {
        lastAttack = System.currentTimeMillis();
    }

    public boolean isDoubleLeftClick() {
        long currentLeftClick = System.currentTimeMillis();

        // Abort if recently punched something
        if (currentLeftClick - lastAttack < 1250) {
            return false;
        }

        boolean isDoubleClick = currentLeftClick - lastLeftClick < 500;
        lastLeftClick = currentLeftClick;

        return isDoubleClick;
    }

    public boolean isDoubleRightClick() {
        long currentRightClick = System.currentTimeMillis();

        boolean isDoubleClick = currentRightClick - lastRightClick < 500;
        lastRightClick = currentRightClick;

        return isDoubleClick;
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

    public boolean hasPower(RoguePower power) {
        return getExperience() >= GuildLevel.getExperienceForLevel(power.getUnlockLevel());
    }

    @Override
    public GuildType getType() {
        return GuildType.ROGUE;
    }
}
