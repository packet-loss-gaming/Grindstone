package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

public class MassBossPlayerKillInfo {
    private final MassBossKillInfo info;
    private final Player player;

    public MassBossPlayerKillInfo(MassBossKillInfo info, Player player) {
        this.info = info;
        this.player = player;
    }

    public MassBossKillInfo getKillInfo() {
        return info;
    }

    public Player getPlayer() {
        return player;
    }
}
