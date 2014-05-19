/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area;

public interface PersistentArena {
    public void writeData(boolean doAsync);
    public void reloadData();
}
