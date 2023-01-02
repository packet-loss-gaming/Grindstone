/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import gg.packetloss.grindstone.util.BossBarProvider;
import gg.packetloss.openboss.entity.EntityDetail;
import org.bukkit.boss.BossBar;

public class RangeWorldMinibossDetail implements EntityDetail, BossBarProvider {
    private final int level;
    private final BossBar bossBar;

    public RangeWorldMinibossDetail(int level, BossBar bossBar) {
        this.level = level;
        this.bossBar = bossBar;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public BossBar getBossBar() {
        return bossBar;
    }
}
