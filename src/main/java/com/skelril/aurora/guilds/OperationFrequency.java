/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.guilds;

public enum OperationFrequency {

    MAXIMUM(4, 1),
    HIGH(3, 5),
    MED(2, 10),
    LOW(1, 30),
    NON_AUTO(0, -1);

    final int level;
    final int seconds;

    OperationFrequency(int level, int seconds) {

        this.level = level;
        this.seconds = seconds;
    }

    public int getLevel() {

        return level;
    }

    public int getSeconds() {

        return seconds;
    }
}
