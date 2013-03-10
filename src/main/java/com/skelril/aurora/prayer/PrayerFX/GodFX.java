package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class GodFX extends AbstractTriggeredPrayer {

    private static final AbstractPrayer[] subFX = new AbstractPrayer[] {
            new ThrownFireballFX(), new InfiniteHungerFX(),
            new InvisibilityFX()
    };
    private static PotionEffect[] effects = new PotionEffect[] {
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 10),
            new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 10),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 10),
            new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 10),
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 10)
    };
    private static PotionEffectType[] removableEffects = new PotionEffectType[] {
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

        for (AbstractPrayer aSubFX : subFX) {
            if (aSubFX instanceof AbstractTriggeredPrayer) {
                ((AbstractTriggeredPrayer) aSubFX).trigger(player);
            }
        }
    }
}
