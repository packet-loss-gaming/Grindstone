/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.jail;

import com.sk89q.commandbook.CommandBook;

import java.util.UUID;

/**
 * Author: Turtle9598
 */
public class Inmate {

    private UUID ID;
    private String name;
    private final String prisonName;
    private final String reason;
    private final long start;
    private final long end;
    private final boolean isMuted;

    public Inmate(String prisonName, String reason, long start, long end, boolean isMuted) {
        this.prisonName = prisonName.trim();
        this.reason = reason.trim();
        this.start = start;
        this.end = end;
        this.isMuted = isMuted;
    }

    public Inmate(UUID ID, String prisonName, String reason, long start, long end, boolean isMuted) {
        this(prisonName, reason, start, end, isMuted);
        this.ID = ID;
    }

    public UUID getID() {
        return ID;
    }

    public void setID(UUID ID) {
        this.ID = ID;
    }

    public String getName() {
        if (name == null || name.isEmpty()) {
            return CommandBook.server().getOfflinePlayer(name).getName();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrisonName() {
        return prisonName;
    }

    public String getReason() {
        return reason.isEmpty() ? null : reason;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public boolean isMuted() {
        return isMuted;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Inmate)) {
            return false;
        }
        Inmate inmate = (Inmate) other;
        return potentialNullEquals(ID, inmate.ID);
    }

    public static boolean potentialNullEquals(Object a, Object b) {
        return (a == null && b == null)
                || a != null && b != null
                && a.equals(b);
    }

    @Override
    public int hashCode() {
        int result = ID.hashCode();
        result = 32 * result;
        return result;
    }
}
