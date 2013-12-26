package com.skelril.aurora.items.specialattack.attacks.ranged.misc;

import com.skelril.aurora.items.specialattack.LocationAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * Created by wyatt on 12/26/13.
 */
public class MobAttack extends LocationAttack implements RangedSpecial {

    private EntityType type;

    public MobAttack(LivingEntity owner, Location target, EntityType type) {
        super(owner, target);
        this.type = type;
    }

    @Override
    public void activate() {


        if (type == EntityType.BAT) {
            inform("Your bow releases a batty attack.");
        } else {
            inform("Your bow releases a " + type.getName().toLowerCase() + " attack.");
        }
    }
}
