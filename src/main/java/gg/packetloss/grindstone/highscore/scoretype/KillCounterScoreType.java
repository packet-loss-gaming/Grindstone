/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

public class KillCounterScoreType extends BasicScoreType {
    protected KillCounterScoreType(int id, boolean gobletEnabled) {
        super(id, gobletEnabled, UpdateMethod.INCREMENTAL, ScoreType.Order.DESC);
    }
}
