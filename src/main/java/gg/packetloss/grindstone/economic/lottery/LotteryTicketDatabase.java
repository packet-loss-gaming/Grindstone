/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import java.util.List;
import java.util.UUID;


public interface LotteryTicketDatabase {
    void addTickets(UUID playerID, int count);
    void addCPUTickets(int count);

    int getTickets(UUID playerID);

    void clearTickets();

    int getTicketCount();

    List<LotteryTicketEntry> getTickets();
}
