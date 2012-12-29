package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.ChatUtil;

/**
 * Author: Turtle9598
 */
public class StarvationFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        if (player.getFoodLevel() > 0) {
            ChatUtil.sendWarning(player, "Tasty...");
            player.setFoodLevel(player.getFoodLevel() - 1);
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
