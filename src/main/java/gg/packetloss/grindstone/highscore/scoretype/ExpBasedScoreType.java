/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import gg.packetloss.grindstone.guild.GuildLevel;

class ExpBasedScoreType extends BasicScoreType {
    protected ExpBasedScoreType(int id) {
        super(id, false, false, Order.DESC);
    }

    @Override
    public String format(long score) {
        return String.valueOf(GuildLevel.getLevel(score));
    }
}
