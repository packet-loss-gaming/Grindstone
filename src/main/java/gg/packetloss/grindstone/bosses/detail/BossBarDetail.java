/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.detail;

import gg.packetloss.grindstone.util.BossBarProvider;
import gg.packetloss.openboss.entity.EntityDetail;
import org.bukkit.boss.BossBar;

public class BossBarDetail implements EntityDetail, BossBarProvider {
    private BossBar bossBar;

    public BossBarDetail(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    @Override
    public BossBar getBossBar() {
        return bossBar;
    }
}
