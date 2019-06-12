/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import gg.packetloss.grindstone.util.player.GenericWealthStore;

import java.util.List;


public interface LotteryTicketDatabase {

    boolean load();

    boolean save();

    void addTickets(String playerName, int count);

    int getTickets(String playerName);

    void clearTickets();

    int getTicketCount();

    List<GenericWealthStore> getTickets();
}
