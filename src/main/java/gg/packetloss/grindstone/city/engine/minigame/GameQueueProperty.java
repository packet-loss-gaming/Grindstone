/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.minigame;

import java.util.Set;

public class GameQueueProperty {

  protected String name;
  protected int team;
  protected Set<Character> flags;

  public GameQueueProperty(String name, int team, Set<Character> flags) {

    this.name = name;
    this.team = team;
    this.flags = flags;
  }
}
