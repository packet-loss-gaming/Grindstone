/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import gg.packetloss.grindstone.util.PlayernameGenerator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

import static gg.packetloss.grindstone.economic.lottery.LotteryTicketDatabase.CPU_ID;

public class LotteryWinner {
    private UUID playerID;
    private double amt;

    public LotteryWinner(UUID playerID, double amt) {
        this.playerID = playerID;
        this.amt = amt;
    }

    public boolean isBot() {
        return playerID.equals(CPU_ID);
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public OfflinePlayer getAsOfflinePlayer() {
        return Bukkit.getOfflinePlayer(playerID);
    }

    public String getName() {
        if (isBot()) {
            return new PlayernameGenerator((long) amt).generate();
        }

        return getAsOfflinePlayer().getName();
    }

    public double getAmt() {
        return amt;
    }
}
