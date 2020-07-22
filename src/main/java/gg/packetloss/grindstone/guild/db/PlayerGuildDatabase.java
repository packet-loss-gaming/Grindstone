/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
