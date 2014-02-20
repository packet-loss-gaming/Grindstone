/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.minigame;

/**
 * User: Wyatt Childers
 * Date: 1/13/14
 */
public enum GameProgress {
    DONE(0),
    INITIALIZED(1),
    ACTIVE(2),
    ENDING(3);

    public int level;

    GameProgress(int level) {

        this.level = level;
    }
}