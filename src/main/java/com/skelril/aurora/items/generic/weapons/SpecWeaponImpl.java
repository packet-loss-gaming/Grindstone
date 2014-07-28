/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.generic.weapons;

import com.skelril.aurora.items.specialattack.SpecialAttack;
import org.bukkit.entity.LivingEntity;

public interface SpecWeaponImpl {
    public SpecialAttack getSpecial(LivingEntity owner, LivingEntity target);
}
