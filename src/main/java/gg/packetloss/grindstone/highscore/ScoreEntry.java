/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class ScoreEntry {
    private UUID playerID;
    private long score;

    public ScoreEntry(UUID playerID, long score) {
        this.playerID = playerID;
        this.score = score;
    }

    public UUID getPlayerUUID() {
        return playerID;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(playerID);
    }

    public long getScore() {
        return score;
    }
}
