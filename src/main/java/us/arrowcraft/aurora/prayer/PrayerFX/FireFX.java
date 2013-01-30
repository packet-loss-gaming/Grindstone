package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.prayer.PrayerType;
import us.arrowcraft.aurora.util.ChatUtil;

/**
 * Author: Turtle9598
 */
public class FireFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.FIRE;
    }

    @Override
    public void add(Player player) {

        if (player.getFireTicks() < 20) {
            ChatUtil.sendWarning(player, "BURN!!!");
            player.setFireTicks((20 * 60));
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
