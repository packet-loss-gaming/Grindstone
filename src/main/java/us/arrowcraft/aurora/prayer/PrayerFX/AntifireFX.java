package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class AntifireFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2);

    public AntifireFX() {

        super(null, effect);
    }
}
