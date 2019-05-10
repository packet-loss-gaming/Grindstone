/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import gg.packetloss.grindstone.util.player.GenericWealthStore;

import java.util.List;


public interface LotteryTicketDatabase {

    public boolean load();

    public boolean save();

    public void addTickets(String playerName, int count);

    public int getTickets(String playerName);

    public void clearTickets();

    public int getTicketCount();

    public List<GenericWealthStore> getTickets();
}
