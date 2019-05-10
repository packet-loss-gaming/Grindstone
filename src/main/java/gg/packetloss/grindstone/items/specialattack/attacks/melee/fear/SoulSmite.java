/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.fear;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;

public class SoulSmite extends EntityAttack implements MeleeSpecial {

    public SoulSmite(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        final double targetHP = target.getHealth() / target.getMaxHealth();

        target.setHealth((targetHP / 2) * target.getMaxHealth());
        server.getScheduler().runTaskLater(inst, () -> {
            if (target.isValid()) {
                double newTargetHP = target.getHealth() / target.getMaxHealth();
                if (newTargetHP < targetHP) {
                    target.setHealth(target.getMaxHealth() * targetHP);
                }
            }
            inform("Your sword releases its grasp on its victim.");
        }, 20 * (int) Math.min(20, target.getMaxHealth() / 5 + 1));
        inform("Your sword steals its victims health for a short time.");
    }
}
