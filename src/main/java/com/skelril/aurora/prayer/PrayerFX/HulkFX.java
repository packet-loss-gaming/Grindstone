package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class HulkFX extends AbstractPrayer {

    private static final AbstractPrayer[] subFX = new AbstractPrayer[] {
            new InfiniteHungerFX()
    };
    private static PotionEffect[] effects = new PotionEffect[] {
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 4),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 30, 4)
    };

    public HulkFX() {

        super(subFX, effects);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.HULK;
    }

    @Override
    public void clean(Player player) {

        super.clean(player);
    }
}