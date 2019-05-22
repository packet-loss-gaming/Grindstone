/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class JailCell {

  private final String name;
  private final String prison;
  private final String world;
  private final int x;
  private final int y;
  private final int z;

  public JailCell(String name, String prison, String world, int x, int y, int z) {

    this.name = name;
    this.prison = prison;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public static boolean potentialNullEquals(Object a, Object b) {

    return (a == null && b == null)
        || a != null && b != null
        && a.equals(b);
  }

  public String getCellName() {

    return name;
  }

  public String getPrisonName() {

    return prison;
  }

  public String getWorldName() {

    return world;
  }

  public int getX() {

    return x;
  }

  public int getY() {

    return y;
  }

  public int getZ() {

    return z;
  }

  public Location getLocation() {

    return new Location(Bukkit.getWorld(world), x, y, z);
  }

  @Override
  public boolean equals(Object other) {

    if (!(other instanceof JailCell)) {
      return false;
    }
    JailCell jailCell = (JailCell) other;
    return potentialNullEquals(this.name, jailCell.name);
  }

  @Override
  public int hashCode() {

    int result = name != null ? name.hashCode() : 0;
    result = 32 * result;
    return result;
  }
}