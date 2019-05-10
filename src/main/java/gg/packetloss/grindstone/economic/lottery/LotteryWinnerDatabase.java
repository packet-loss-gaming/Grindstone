/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import java.util.List;


public interface LotteryWinnerDatabase {

    public boolean load();

    public boolean save();

    public void addWinner(String name, double amount);

    public List<LotteryWinner> getRecentWinner(int limit);
}
