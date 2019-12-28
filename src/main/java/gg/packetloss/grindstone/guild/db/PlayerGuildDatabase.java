package gg.packetloss.grindstone.guild.db;

import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.state.InternalGuildState;

import java.util.Optional;
import java.util.UUID;

public interface PlayerGuildDatabase {
    Optional<InternalGuildState> loadGuild(UUID playerID);
    Optional<InternalGuildState> loadGuild(UUID playerID, GuildType type);
    void updateActive(UUID playerID, InternalGuildState guildState);
}
