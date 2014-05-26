/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public abstract class RogueEvent extends PlayerEvent {
    public RogueEvent(Player who) {
        super(who);
    }
}
