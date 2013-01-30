package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class NightVisionFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 600, 1);

    public NightVisionFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.NIGHTVISION;
    }
}
