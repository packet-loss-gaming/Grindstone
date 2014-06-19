/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.modifiers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Modifier implements Serializable {

    // Integer key is the DurationType.id() and long is the end time
    private Map<Integer, Long> times = new HashMap<>();

    public void extend(ModifierType type, long amount) {
        Long time = times.get(type.id());
        long curTime = System.currentTimeMillis();
        if (time != null && time > curTime) {
            time += amount;
        } else {
            time = curTime + amount;
        }

        times.put(type.id(), time);
    }

    public boolean isActive(ModifierType type) {
        return status(type) != 0;
    }

    public long status(ModifierType type) {
        Long time = times.get(type.id());
        return time != null ? Math.max(time - System.currentTimeMillis(), 0) : 0;
    }
}
