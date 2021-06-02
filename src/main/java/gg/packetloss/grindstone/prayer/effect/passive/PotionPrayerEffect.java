/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionPrayerEffect implements PassivePrayerEffect {
    private static final int REAPPLY_THRESHOLD = 20 * 3;
    private static final int POTION_DURATION = 20 * 60;

    private final PotionEffectType desiredEffectType;
    private final int amplifier;

    public PotionPrayerEffect(PotionEffectType desiredEffectType, int amplifier) {

        this.desiredEffectType = desiredEffectType;
        this.amplifier = amplifier;
    }

    private boolean shouldReapply(Player player) {
        PotionEffect potionEffect = player.getPotionEffect(desiredEffectType);
        if (potionEffect == null) {
            return true;
        }

        // Re-add potion effects that are about to expire
        if (potionEffect.getDuration() < REAPPLY_THRESHOLD) {
            return true;
        }

        // This version of the potion is better
        if (potionEffect.getAmplifier() < amplifier) {
            return true;
        }

        // They already have it and it's not about to expire
        return false;
    }

    @Override
    public void trigger(Player player) {
        if (shouldReapply(player)) {
            player.removePotionEffect(desiredEffectType);

            PotionEffect newEffect = new PotionEffect(desiredEffectType, POTION_DURATION, amplifier);
            player.addPotionEffect(newEffect);
        }
    }

    @Override
    public void strip(Player player) {
        player.removePotionEffect(desiredEffectType);
    }
}
