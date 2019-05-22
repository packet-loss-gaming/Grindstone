/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items;

import com.sk89q.commandbook.session.PersistentSession;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CustomItemSession extends PersistentSession {

  private static final long MAX_AGE = TimeUnit.DAYS.toMillis(3);

  private HashMap<SpecType, Long> specMap = new HashMap<>();
  private LinkedList<Location> recentDeathLocations = new LinkedList<>();

  protected CustomItemSession() {
    super(MAX_AGE);
  }

  public void updateSpec(SpecType type, long delay) {

    specMap.put(type, System.currentTimeMillis() + delay);
  }

  public boolean canSpec(SpecType type) {

    return !specMap.containsKey(type) || System.currentTimeMillis() - specMap.get(type) >= 0;
  }

  public void addDeathPoint(Location deathPoint) {

    recentDeathLocations.add(0, deathPoint.clone());
    while (recentDeathLocations.size() > 5) {
      recentDeathLocations.pollLast();
    }
  }

  public Location getRecentDeathPoint() {

    return recentDeathLocations.poll();
  }
}