package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class InfiniteHungerFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setExhaustion(0);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
