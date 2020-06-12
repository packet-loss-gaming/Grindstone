package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildLevel;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import org.apache.commons.lang.Validate;

public abstract class InternalGuildState {
    private boolean enabled = false;
    private double experience;
    private double virtualExperience = -1;

    private DebounceHandle<Double>.State expNoticeDebounce;

    public InternalGuildState(double experience) {
        this.experience = experience;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public boolean hasVirtualExperience() {
        return virtualExperience != -1;
    }

    public double getVirtualExperience() {
        Validate.isTrue(hasVirtualExperience());
        return virtualExperience;
    }

    public void setVirtualLevel(int level) {
        Validate.isTrue(level > 0);
        this.virtualExperience = GuildLevel.getExperienceForLevel(level);
    }

    public void clearVirtualLevel() {
        this.virtualExperience = -1;
    }

    protected boolean hasLevelForPower(GuildPower power) {
        double exp = hasVirtualExperience() ? getVirtualExperience() : getExperience();
        return exp >= GuildLevel.getExperienceForLevel(power.getUnlockLevel());
    }

    public void setExpNoticeDebounce(DebounceHandle<Double>.State expNoticeDebounce) {
        this.expNoticeDebounce = expNoticeDebounce;
    }

    public DebounceHandle<Double>.State getExpNoticeDebounce() {
        return expNoticeDebounce;
    }

    public abstract GuildType getType();

    public abstract StateSettings getSettings();
}
