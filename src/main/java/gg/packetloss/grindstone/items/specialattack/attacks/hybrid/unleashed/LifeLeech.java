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

public class LifeLeech extends EntityAttack implements MeleeSpecial, RangedSpecial {

  public LifeLeech(LivingEntity owner, LivingEntity target) {
    super(owner, target);
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
      target.setHealth(target.getMaxHealth() * ownerHP);
      owner.setHealth(Math.min(ownerMax, ownerMax * targetHP * 1.1));
      inform("You leech the health of your foe.");
    }
  }
}
