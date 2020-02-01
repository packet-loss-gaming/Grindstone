/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LifeLeech extends EntityAttack implements MeleeSpecial, RangedSpecial {

    public LifeLeech(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        final double ownerMax = owner.getMaxHealth();

        final double ownerHP = owner.getHealth() / ownerMax;
        final double targetHP = target.getHealth() / target.getMaxHealth();

        if (ownerHP > targetHP) {
            owner.setHealth(Math.min(ownerMax, ownerMax * (ownerHP + .1)));
            inform("Your weapon heals you.");
        } else {
            double newHealth = target.getMaxHealth() * ownerHP;
            if (!(target instanceof Player)) {
                newHealth = Math.max(target.getHealth() - (20 * (1 - ownerHP)), newHealth);
            }

            target.setHealth(newHealth);
            owner.setHealth(Math.min(ownerMax, ownerMax * targetHP * 1.1));
            inform("You leech the health of your foe.");
        }
    }
}
