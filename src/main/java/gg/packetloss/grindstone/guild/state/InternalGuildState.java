package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildType;

public abstract class InternalGuildState {
    private boolean enabled = false;
    private long experience;

    public InternalGuildState(long experience) {
        this.experience = experience;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public abstract GuildType getType();
}
