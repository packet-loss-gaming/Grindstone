/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.MirageArena;

public class ArenaVote {
    private String arena;
    private int votes;

    public ArenaVote(String arena) {
        this.arena = arena;
    }

    public String getArena() {
        return arena;
    }

    public void addVote() {
        ++votes;
    }

    public int getVotes() {
        return votes;
    }
}
