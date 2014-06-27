/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.player;

import com.skelril.aurora.admin.AdminComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Collectors;

public class AdminToolkit {

    private AdminComponent admin;

    public AdminToolkit(AdminComponent admin) {
        this.admin = admin;
    }

    public <T extends Entity> Collection<T> removeAdmin(Collection<T> entities) {
        return entities.stream()
                .filter(e -> !(e instanceof Player && admin.isAdmin((Player) e)))
                .collect(Collectors.toSet());
    }
}
