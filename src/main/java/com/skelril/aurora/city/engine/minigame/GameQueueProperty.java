/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.minigame;

import java.util.Set;

/**
 * User: Wyatt Childers
 * Date: 1/24/14
 */
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
