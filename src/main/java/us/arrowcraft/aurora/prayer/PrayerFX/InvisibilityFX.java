package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class InvisibilityFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 600, 1);

    public InvisibilityFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.INVISIBILITY;
    }
}
