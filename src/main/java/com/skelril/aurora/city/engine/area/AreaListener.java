/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area;

import org.bukkit.event.Listener;

public abstract class AreaListener<Area extends AreaComponent<?>> implements Listener {

    protected final Area parent;

    public AreaListener(Area parent) {
        this.parent = parent;
    }
}
