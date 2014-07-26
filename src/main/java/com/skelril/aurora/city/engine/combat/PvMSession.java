/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.combat;

import com.sk89q.commandbook.session.PersistentSession;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PvMSession extends PersistentSession {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private UUID lastAttacked = null;

    protected PvMSession() {
        super(MAX_AGE);
    }

    public boolean checkLast(UUID lastAttacked) {
        if (this.lastAttacked == lastAttacked) {
            return true;
        }
        this.lastAttacked = lastAttacked;
        return false;
    }
}
