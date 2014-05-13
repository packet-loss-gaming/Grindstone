/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.player;

import com.skelril.aurora.admin.AdminComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class AdminUtil {
    public static <T extends Entity> T[] removeAdmin(T[] entities, AdminComponent admin) {
        List<T> returned = new ArrayList<>();
        for (T e : entities) {
            if (e instanceof Player && admin.isAdmin((Player) e)) continue;
            returned.add(e);
        }
        //noinspection unchecked
        return returned.toArray((T[]) Array.newInstance(entities.getClass(), returned.size()));
    }
}
