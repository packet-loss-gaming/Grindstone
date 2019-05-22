/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.modifiers;

public enum ModifierType {

  // !!! IMPORTANT !!!
  // DO NOT CHANGE IDS HERE
  // ONCE THEY HAVE BEEN SET
  DOUBLE_CURSED_ORES(0, "Double Cursed Mine Ores"),
  DOUBLE_WILD_ORES(1, "Double Wilderness Ores"),
  DOUBLE_WILD_DROPS(2, "Double Wilderness Drops"),
  QUAD_GOLD_RUSH(3, "Quadruple Gold Rush"),
  TRIPLE_FACTORY_PRODUCTION(4, "Triple Factory Production"),
  HEXA_FACTORY_SPEED(5, "Hextuple Factory Speed"),
  NONUPLE_MIRAGE_GOLD(6, "Nonuple Mirage Arena Gold");

  final int id;
  final String fname;

  ModifierType(int id, String fname) {
    this.id = id;
    this.fname = fname;
  }

  public int id() {
    return id;
  }

  public String fname() {
    return fname;
  }
}
