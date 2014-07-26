/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.combat;

import com.sk89q.commandbook.session.PersistentSession;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PvPSession extends PersistentSession {

    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    // Flag booleans
    private boolean hasPvPOn = false;
    private boolean useSafeSpots = true;

    // Punishment booleans & data
    private boolean wasKicked = false;
    private boolean punishNextLogin = false;

    private long nextFreePoint = 0;

    protected PvPSession() {

        super(MAX_AGE);
    }

    public Player getPlayer() {

        CommandSender sender = super.getOwner();
        return sender instanceof Player ? (Player) sender : null;
    }

    public boolean hasPvPOn() {

        return hasPvPOn;
    }

    public void setPvP(boolean hasPvPOn) {

        this.hasPvPOn = hasPvPOn;
    }

    public boolean useSafeSpots() {

        return useSafeSpots;
    }

    public void useSafeSpots(boolean useSafeSpots) {

        this.useSafeSpots = useSafeSpots;
    }

    public boolean punishNextLogin() {

        return punishNextLogin && !wasKicked;
    }

    public void punishNextLogin(boolean witherNextLogin) {

        this.punishNextLogin = witherNextLogin;
    }

    public void wasKicked(boolean wasKicked) {

        this.wasKicked = wasKicked;
    }

    public boolean recentlyHit() {

        return System.currentTimeMillis() < nextFreePoint;
    }

    public void updateHit() {

        nextFreePoint = System.currentTimeMillis() + 7000;
    }

    public void resetHit() {
        nextFreePoint = 0;
    }
}