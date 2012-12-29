package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;


/**
 * Author: Turtle9598
 */
public class ArrowFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        Location eyeLoc = player.getEyeLocation();

        eyeLoc.setX(eyeLoc.getX());
        eyeLoc.setY(eyeLoc.getY());
        eyeLoc.setZ(eyeLoc.getZ());
        player.getWorld().spawn(eyeLoc, Arrow.class);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
