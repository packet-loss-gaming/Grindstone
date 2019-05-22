/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed;

import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HealingLight extends EntityAttack implements MeleeSpecial {

  public HealingLight(LivingEntity owner, LivingEntity target) {
    super(owner, target);
  }

  @Override
  public void activate() {

    if (owner instanceof Player) {
      SERVER.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
    }

    owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + 5));
    for (int i = 0; i < 4; i++) {
      target.getWorld().playEffect(target.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
    }

    DamageUtil.damage(owner, target, 20);
    inform("Your weapon glows dimly.");
  }
}
