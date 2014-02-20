/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/**
 * Author: Turtle9598
 */
public abstract class AbstractTriggeredEffect extends AbstractEffect {

    private final Class triggerClass;

    public AbstractTriggeredEffect(Class triggerClass) {

        this.triggerClass = triggerClass;
    }

    public AbstractTriggeredEffect(Class triggerClass, AbstractEffect[] subFX) {

        super(subFX);
        this.triggerClass = triggerClass;
    }

    public AbstractTriggeredEffect(Class triggerClass, AbstractEffect[] subFX, PotionEffect... effects) {

        super(subFX, effects);
        this.triggerClass = triggerClass;
    }

    public Class getTriggerClass() {

        return triggerClass;
    }

    public abstract void trigger(Player player);

}
