/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Famine extends EntityAttack implements MeleeSpecial {

  public Famine(LivingEntity owner, LivingEntity target) {
    super(owner, target);
  }

  @Override
  public void activate() {

    if (target instanceof Player) {
      ((Player) target).setFoodLevel((int) (((Player) target).getFoodLevel() * .85));
      ((Player) target).setSaturation(0);
    } else {
      target.setMaxHealth(target.getMaxHealth() * .9);
    }

    inform("You drain the stamina of your foe.");
  }
}
