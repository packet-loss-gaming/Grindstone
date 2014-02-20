/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

/**
 * User: Wyatt Childers
 * Date: 10/12/13
 */
public interface PersistentArena {

    public void writeData(boolean doAsync);

    public void reloadData();
}
