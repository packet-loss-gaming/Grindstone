/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.skywars;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SkyWarsGameState {
    private Map<Player, SkyWarsProfile> playerProfiles = new HashMap<>();

    public boolean hasParticipants() {
        return !getPlayers().isEmpty();
    }

    public SkyWarsProfile get(Player player) {
        return playerProfiles.get(player);
    }

    public Collection<Player> getPlayers() {
        return playerProfiles.keySet();
    }

    public Collection<SkyWarsProfile> getProfiles() {
        return playerProfiles.values();
    }

    public void addPlayer(Player player) {
        playerProfiles.put(player, new SkyWarsProfile());
    }

    public void removePlayer(Player player) {
        playerProfiles.remove(player);
    }

    public boolean containsPlayer(Player player) {
        return playerProfiles.containsKey(player);
    }
}
