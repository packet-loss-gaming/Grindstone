/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.modifiers;

public enum ModifierType {

    // !!! IMPORTANT !!!
    // DO NOT CHANGE IDS HERE
    // ONCE THEY HAVE BEEN SET
    DOUBLE_CURSED_ORES(0, "Double Cursed Mine Ores"),
    DOUBLE_WILD_ORES(1, "Double Wilderness Ores"),
    DOUBLE_WILD_DROPS(2, "Double Wilderness Drops"),
    QUAD_GOLD_RUSH(3, "Quadruple Gold Rush"),
    TRIPLE_FACTORY_PRODUCTION(4, "Triple Factory Production"),
    HEXA_FACTORY_SPEED(5, "Hextuple Factory Speed");

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
