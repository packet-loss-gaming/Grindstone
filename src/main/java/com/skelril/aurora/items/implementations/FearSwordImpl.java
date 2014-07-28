/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.items.generic.weapons.SpecWeaponImpl;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.fear.Curse;
import com.skelril.aurora.items.specialattack.attacks.melee.fear.*;
import com.skelril.aurora.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;

public class FearSwordImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, LivingEntity target) {
        switch (ChanceUtil.getRandom(6)) {
            case 1:
                return new Confuse(owner, target);
            case 2:
                return new FearBlaze(owner, target);
            case 3:
                return new Curse(owner, target);
            case 4:
                return new Weaken(owner, target);
            case 5:
                return new Decimate(owner, target);
            case 6:
                return new SoulSmite(owner, target);
        }
        return null;
    }
}
