package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class RacketFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.RACKET;
    }

    @Override
    public void add(Player player) {

        player.playSound(player.getLocation(), Sound.ZOMBIE_WOODBREAK, 1, 0);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
