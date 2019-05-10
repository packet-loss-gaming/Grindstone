/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GodFX extends AbstractTriggeredEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
            new ThrownFireballFX(), new InfiniteHungerFX(),
            new InvisibilityFX()
    };
    private static PotionEffect[] effects = new PotionEffect[]{
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 10),
            new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 10),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 10),
            new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 10),
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 10)
    };
    private static PotionEffectType[] removableEffects = new PotionEffectType[]{
            PotionEffectType.CONFUSION, PotionEffectType.BLINDNESS, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.SLOW
    };

    public GodFX() {

        super(PlayerInteractEvent.class, subFX, effects);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.GOD;
    }

    @Override
    public void clean(Player player) {

        super.clean(player);

        for (PotionEffectType removableEffect : removableEffects) {
            player.removePotionEffect(removableEffect);
        }
    }

    @Override
    public void trigger(Player player) {

        for (AbstractEffect aSubFX : subFX) {
            if (aSubFX instanceof AbstractTriggeredEffect) {
                ((AbstractTriggeredEffect) aSubFX).trigger(player);
            }
        }
    }
}
