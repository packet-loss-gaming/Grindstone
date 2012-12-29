package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class HealthFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 1);

    public HealthFX() {

        super(null, effect);
    }
}
