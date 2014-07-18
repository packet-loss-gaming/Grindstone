/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.MirageArena;

import com.sk89q.commandbook.session.PersistentSession;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MirageSession extends PersistentSession {

    private String vote;
    private Set<String> ignored = new HashSet<>();
    private double dmgTaken = 0;

    protected MirageSession() {
        super(TimeUnit.MINUTES.toMillis(30));
    }

    public void vote(String vote) {
        this.vote = vote;
    }

    public String getVote() {
        return vote;
    }

    public boolean isIgnored(String player) {
        return ignored.contains(player);
    }

    public void ignore(String player) {
        ignored.add(player);
    }

    public void unignore(String player) {
        ignored.remove(player);
    }

    public double getDamage() {
        return dmgTaken;
    }

    public void resetDamage() {
        dmgTaken = 0;
    }

    public void addDamage(double amt) {
        dmgTaken += amt;
    }
}
