package gg.packetloss.grindstone.highscore;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class ScoreEntry {
    private UUID playerID;
    private int score;

    public ScoreEntry(UUID playerID, int score) {
        this.playerID = playerID;
        this.score = score;
    }

    public UUID getPlayerUUID() {
        return playerID;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(playerID);
    }

    public int getScore() {
        return score;
    }
}
