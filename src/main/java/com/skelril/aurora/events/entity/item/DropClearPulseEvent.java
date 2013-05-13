package com.skelril.aurora.events.entity.item;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Author: Turtle9598
 */
public class DropClearPulseEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final World world;
    private int secondsLeft;

    public DropClearPulseEvent(World world, int secondsLeft) {

        this.world = world;
        this.secondsLeft = secondsLeft;
    }

    public World getWorld() {

        return world;
    }

    public int getSecondsLeft() {

        return secondsLeft;
    }

    public void setSecondsLeft(int secondsLeft) {

        this.secondsLeft = Math.min(120, secondsLeft);
    }

    public int getCurrentSecond() {

        return secondsLeft + 1;
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
