package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.GuildType;

public abstract class InternalGuildState {
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract GuildType getType();
}
