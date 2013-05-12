package com.skelril.aurora.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RapidHitEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private int damage;

    public RapidHitEvent(Player player, int damage) {

        super(player);
        this.damage = damage;
    }

    public void setDamage(int damage) {

        this.damage = damage;
    }

    public int getDamage() {

        return damage;
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}