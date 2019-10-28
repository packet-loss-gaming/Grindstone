/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import gg.packetloss.grindstone.util.PlayernameGenerator;

import static gg.packetloss.grindstone.economic.lottery.LotteryTicketDatabase.CPU_NAME;

public class LotteryWinner {
    private String name;
    private double amt;

    public LotteryWinner(String name, double amt) {
        this.name = name;
        this.amt = amt;
    }

    public boolean isBot() {
        return name.equals(CPU_NAME);
    }

    public String getName() {
        if (isBot()) {
            return new PlayernameGenerator((long) amt).generate();
        }

        return name;
    }

    public double getAmt() {
        return amt;
    }
}
