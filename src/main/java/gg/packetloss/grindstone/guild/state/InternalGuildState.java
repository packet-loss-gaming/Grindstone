package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildLevel;
import gg.packetloss.grindstone.guild.GuildType;

public abstract class InternalGuildState {
    private boolean enabled = false;
    private double experience;

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

    public int getLevel() {
        return GuildLevel.getLevel(getExperience());
    }

    public abstract GuildType getType();
}
