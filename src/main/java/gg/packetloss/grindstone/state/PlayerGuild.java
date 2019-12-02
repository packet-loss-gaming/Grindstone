package gg.packetloss.grindstone.state;

import gg.packetloss.grindstone.guild.GuildType;

public class PlayerGuild {
    private GuildType guildType;
    private boolean powersEnabled;

    public PlayerGuild(GuildType guildType, boolean powersEnabled) {
        this.guildType = guildType;
        this.powersEnabled = powersEnabled;
    }

    public GuildType getGuildType() {
        return guildType;
    }

    public boolean arePowersEnabled() {
        return powersEnabled;
    }
}
