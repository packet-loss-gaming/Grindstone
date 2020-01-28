package gg.packetloss.grindstone.bosses.detail;

import org.bukkit.boss.BossBar;

public class BossBarDetail extends GenericDetail {
    private BossBar bossBar;

    public BossBarDetail(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public BossBar getBossBar() {
        return bossBar;
    }
}
