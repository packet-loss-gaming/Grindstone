/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.fear;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Weaken extends EntityAttack implements MeleeSpecial {

    public Weaken(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        int duration = (int) Math.min(20 * 60 * 5, owner.getHealth() * 18);
        target.removePotionEffect(PotionEffectType.WEAKNESS);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1));
        owner.removePotionEffect(PotionEffectType.STRENGTH);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1));

        inform("Your sword leaches strength from its victim.");
    }
}
