/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.EntityUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NecrosisFX extends AbstractEffect {

    private LivingEntity beneficiary = null;

    public void setBeneficiary(LivingEntity beneficiary) {
        this.beneficiary = beneficiary;
    }

    @Override
    public void add(Player player) {
        EntityUtil.heal(beneficiary, 1);
        EntityUtil.forceDamage(player, 1);
    }

    @Override
    public PrayerType getType() {
        return PrayerType.NECROSIS;
    }
}
