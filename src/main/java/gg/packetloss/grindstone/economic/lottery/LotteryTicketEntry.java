/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import java.util.UUID;

public class LotteryTicketEntry {
    private final UUID playerID;
    private final int ticketCount;

    public LotteryTicketEntry(UUID playerID, int ticketCount) {
        this.playerID = playerID;
        this.ticketCount = ticketCount;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public int getTicketCount() {
        return ticketCount;
    }
}
