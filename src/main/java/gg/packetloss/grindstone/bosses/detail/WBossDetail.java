/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.detail;

import com.skelril.OSBL.entity.EntityDetail;

public class WBossDetail implements EntityDetail {

    private int level;

    public WBossDetail(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
