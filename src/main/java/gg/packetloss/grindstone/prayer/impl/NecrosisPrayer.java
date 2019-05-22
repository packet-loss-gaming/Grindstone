/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.EntityUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NecrosisPrayer extends AbstractPrayer {

  private final LivingEntity beneficiary;

  public NecrosisPrayer(LivingEntity beneficiary) {
    Validate.notNull(beneficiary);

    this.beneficiary = beneficiary;
  }

  private boolean isFullyHealed() {
    return beneficiary.getHealth() == beneficiary.getMaxHealth();
  }

  private boolean isBlockedPvP(Player player) {
    return beneficiary instanceof Player && !PvPComponent.allowsPvP((Player) beneficiary, player);
  }

  @Override
  public void add(Player player) {
    if (isFullyHealed()) {
      return;
    }
    if (isBlockedPvP(player)) {
      return;
    }

    EntityUtil.heal(beneficiary, 1);
    EntityUtil.forceDamage(player, 1);
  }

  @Override
  public PrayerType getType() {
    return PrayerType.NECROSIS;
  }
}
