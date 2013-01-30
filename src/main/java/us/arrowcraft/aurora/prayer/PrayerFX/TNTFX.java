package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class TNTFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.TNT;
    }

    @Override
    public void add(Player player) {

        Location playerLoc = player.getLocation();
        player.getWorld().createExplosion(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), 1, false, false);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
