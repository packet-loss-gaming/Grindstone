/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

public class LotteryWinner {
  private String name;
  private double amt;

  public LotteryWinner(String name, double amt) {
    this.name = name;
    this.amt = amt;
  }

  public String getName() {
    return name;
  }

  public double getAmt() {
    return amt;
  }
}
