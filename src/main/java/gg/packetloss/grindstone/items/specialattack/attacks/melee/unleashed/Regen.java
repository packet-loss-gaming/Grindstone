/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Regen extends EntityAttack implements MeleeSpecial {

    public Regen(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    public int getNewDuration(PotionEffect existingEffect) {
        int duration = (int) (EntityUtil.getHealth(owner, target) * 10);
        if (existingEffect != null) {
            int existingDuration = existingEffect.getDuration();
            // If this is a lower tier potion don't give the full duration
            if (existingEffect.getAmplifier() < 1) {
                existingDuration *= .40;
            }
            duration += existingDuration;
        }
        return (int) Math.min(TimeUtil.convertMinutesToTicks(2), duration);
    }

    @Override
    public void activate() {
        PotionEffect effect = owner.getPotionEffect(PotionEffectType.REGENERATION);

        int duration = getNewDuration(effect);
        if (effect != null) {
            owner.removePotionEffect(PotionEffectType.REGENERATION);
        }
        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1));

        inform("You gain a healing aura.");
    }
}
