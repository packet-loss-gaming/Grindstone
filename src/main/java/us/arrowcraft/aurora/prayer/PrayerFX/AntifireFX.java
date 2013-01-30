package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class AntifireFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2);

    public AntifireFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.ANTIFIRE;
    }
}
