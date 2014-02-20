/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

import org.bukkit.entity.LivingEntity;

/**
 * Author: Turtle9598
 */
public interface BossArena extends MonitoredArena {

    public boolean isBossSpawned();

    public void spawnBoss();

    public LivingEntity getBoss();
}
