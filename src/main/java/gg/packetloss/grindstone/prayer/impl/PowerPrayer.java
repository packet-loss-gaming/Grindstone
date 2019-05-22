/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerPrayer extends AbstractPrayer {

  private static final AbstractPrayer[] SUB_PRAYERS = new AbstractPrayer[] {
      new InfiniteHunger()
  };
  private static PotionEffect[] POTIONS = new PotionEffect[] {
      new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 2),
      new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 2),
      new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2),
      new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2),
      new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2)
  };
  private static PotionEffectType[] REMOVEABLE_POTION_TYPES = new PotionEffectType[] {
      PotionEffectType.CONFUSION, PotionEffectType.BLINDNESS, PotionEffectType.WEAKNESS,
      PotionEffectType.POISON, PotionEffectType.SLOW
  };

  public PowerPrayer() {

    super(SUB_PRAYERS, POTIONS);
  }

  @Override
  public PrayerType getType() {

    return PrayerType.POWER;
  }

  @Override
  public void clean(Player player) {

    super.clean(player);

    for (PotionEffectType removableEffect : REMOVEABLE_POTION_TYPES) {
      player.removePotionEffect(removableEffect);
    }
  }
}
