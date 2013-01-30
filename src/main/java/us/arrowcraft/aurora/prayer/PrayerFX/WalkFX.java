package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class WalkFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 20 * 600, 2);

    public WalkFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.WALK;
    }
}
