/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.combat;

import org.bukkit.entity.Player;

public abstract class PvPScope {
    public abstract boolean isApplicable(Player player);
    public abstract boolean allowed(Player attacker, Player defender);

    public boolean checkFor(Player attacker, Player defender) {
        // noinspection SimplifiableIfStatement
        if (isApplicable(attacker) && isApplicable(defender)) {
            return allowed(attacker, defender);
        }
        return true;
    }
}
