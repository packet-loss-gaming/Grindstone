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
