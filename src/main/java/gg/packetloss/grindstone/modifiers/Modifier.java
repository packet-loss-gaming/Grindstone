/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.modifiers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Modifier implements Serializable {

  // Integer key is the DurationType.id() and long is the end time
  private Map<Integer, Long> times = new HashMap<>();

  public void extend(ModifierType type, long amount) {
    Long time = times.get(type.id());
    long curTime = System.currentTimeMillis();
    if (time != null && time > curTime) {
      time += amount;
    } else {
      time = curTime + amount;
    }

    times.put(type.id(), time);
  }

  public boolean isActive(ModifierType type) {
    return status(type) != 0;
  }

  public long status(ModifierType type) {
    Long time = times.get(type.id());
    return time != null ? Math.max(time - System.currentTimeMillis(), 0) : 0;
  }
}
