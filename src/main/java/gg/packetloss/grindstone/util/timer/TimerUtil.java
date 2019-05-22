/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.timer;

public class TimerUtil {

  public static boolean matchesFilter(int entry, int min, int divisible) {

    return entry > 0 && entry % divisible == 0 || entry <= min && entry > 0;
  }
}
