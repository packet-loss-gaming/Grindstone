/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractPrayer {

  private final Set<PotionEffect> effects = new HashSet<>();
  private final AbstractPrayer[] subFX;

  public AbstractPrayer() {

    this.subFX = null;
  }

  public AbstractPrayer(AbstractPrayer[] subFX) {

    this.subFX = subFX;
  }

  public AbstractPrayer(AbstractPrayer[] subFX, PotionEffect... effects) {

    this.subFX = subFX;
    Collections.addAll(this.effects, effects);
  }

  public abstract PrayerType getType();

  public Set<PotionEffect> getPotionEffects() {

    return effects;
  }

  public void add(Player player) {

    if (subFX != null) {
      for (AbstractPrayer aSubFX : subFX) {
        aSubFX.add(player);
      }
    }
    player.addPotionEffects(effects);
  }

  public void clean(Player player) {

    if (subFX != null) {
      for (AbstractPrayer aSubFX : subFX) {
        aSubFX.clean(player);
      }
    }
    for (PotionEffect effect : effects) {

      player.removePotionEffect(effect.getType());
    }
  }

  public void kill(Player player) {

    // Do nothing unless implemented
  }
}
