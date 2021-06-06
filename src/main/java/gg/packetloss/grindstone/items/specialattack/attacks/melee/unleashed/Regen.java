/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Regen extends EntityAttack implements MeleeSpecial {

    public Regen(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (EntityUtil.getHealth(owner, target) * 10), 2), true);

        inform("You gain a healing aura.");
    }
}
