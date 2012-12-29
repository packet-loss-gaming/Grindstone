package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class GodFX extends AbstractPrayer implements Throwable {

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

        super(subFX, effects);
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
            if (aSubFX instanceof Throwable) {
                ((Throwable) aSubFX).trigger(player);
            }
        }
    }
}
