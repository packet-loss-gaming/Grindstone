/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.items.generic.weapons.SpecWeaponImpl;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.EvilFocus;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.Speed;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.DoomBlade;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.HealingLight;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.Regen;
import com.skelril.aurora.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;

public class UnleashedSwordImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, LivingEntity target) {
        switch (ChanceUtil.getRandom(6)) {
            case 1:
                return new EvilFocus(owner, target);
            case 2:
                return new HealingLight(owner, target);
            case 3:
                return new Speed(owner, target);
            case 4:
                return new Regen(owner, target);
            case 5:
                return new DoomBlade(owner, target);
            case 6:
                return new LifeLeech(owner, target);
        }
        return null;
    }
}
