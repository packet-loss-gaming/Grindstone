package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class PoisonFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.POISON, 20 * 600, 2);

    public PoisonFX() {

        super(null, effect);
    }
}
