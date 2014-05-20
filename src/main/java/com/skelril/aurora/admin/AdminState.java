/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.admin;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Turtle9598
 */
public enum AdminState {
    MEMBER,
    MODERATOR(MEMBER),
    ADMIN(MODERATOR),
    SYSOP(ADMIN);

    private final AdminState child;
    private Set<AdminState> states = new HashSet<>();

    AdminState() {
        child = null;
    }

    AdminState(AdminState child) {
        this.child = child;
        addState(child);
    }

    private void addState(AdminState state) {
        if (state == null) return;
        states.add(state);
        addState(state.child);
    }

    public boolean isAbove(AdminState state) {
        return this.equals(state) || states.contains(state);
    }
}
