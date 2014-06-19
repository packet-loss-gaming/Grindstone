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
    DOUBLE_CURSED_ORES(0),
    DOUBLE_WILD_ORES(1),
    DOUBLE_WILD_DROPS(2),
    DOUBLE_GOLD_RUSH(3),
    TRIPLE_FACTORY_PRODUCTION(4),
    HEXA_FACTORY_SPEED(5);

    final int id;
    ModifierType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}
