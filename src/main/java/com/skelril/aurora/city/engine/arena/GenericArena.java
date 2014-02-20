/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

/**
 * Author: Turtle9598
 */
public abstract interface GenericArena extends Runnable {

    public void run();

    public void disable();

    public String getId();

    public void equalize();

    public ArenaType getArenaType();

}
