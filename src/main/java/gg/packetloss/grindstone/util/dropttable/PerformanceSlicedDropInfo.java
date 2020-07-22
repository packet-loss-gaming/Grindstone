/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

public class PerformanceSlicedDropInfo {
    private final PerformanceDropTable.SliceInfo sliceInfo;
    private final Player player;

    public PerformanceSlicedDropInfo(PerformanceDropTable.SliceInfo sliceInfo, Player player) {
        this.sliceInfo = sliceInfo;
        this.player = player;
    }

    public PerformanceKillInfo getKillInfo() {
        return sliceInfo.killInfo;
    }

    public int getSlicedPoints() {
        return sliceInfo.points;
    }

    public Player getPlayer() {
        return player;
    }
}
