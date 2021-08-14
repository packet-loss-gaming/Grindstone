/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MagicChain extends EntityAttack implements RangedSpecial {

    public MagicChain(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        target.removePotionEffect(PotionEffectType.SLOW);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (owner.getHealth() * 18), 2));
        target.removePotionEffect(PotionEffectType.WEAKNESS);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) (owner.getHealth() * 18), 2));

        inform("Your bow slows its victim.");
    }
}
