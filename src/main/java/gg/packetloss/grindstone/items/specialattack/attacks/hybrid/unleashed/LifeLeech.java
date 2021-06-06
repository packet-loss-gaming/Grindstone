/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class LifeLeech extends EntityAttack implements MeleeSpecial, RangedSpecial {

    public LifeLeech(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private double getAttemptedDamage() {
        double healthDiff = owner.getMaxHealth() - owner.getHealth();
        return Math.min(EntityUtil.getHealth(owner, target), Math.max(5, healthDiff / 2));
    }

    @Override
    public void activate() {
        double health = EntityUtil.getHealth(owner, target);
        SpecialAttackFactory.processDamage(owner, target, this, getAttemptedDamage());
        double leachedHealth = Math.max(0, health - EntityUtil.getHealth(owner, target));

        EntityUtil.heal(owner, leachedHealth);
        inform("You leach health from your opponent.");
    }
}
