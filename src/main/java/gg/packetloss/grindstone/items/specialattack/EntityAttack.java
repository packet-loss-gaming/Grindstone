/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public abstract class EntityAttack extends SpecialAttack {

  protected LivingEntity target;

  public EntityAttack(LivingEntity owner, LivingEntity target) {
    super(owner);
    this.target = target;
  }

  @Override
  public LivingEntity getTarget() {

    return target;
  }

  @Override
  public Location getLocation() {

    return target.getLocation();
  }
}
