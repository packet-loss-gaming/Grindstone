package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class CannonFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        Location eyeLoc = player.getLocation();

        eyeLoc.setX(eyeLoc.getX());
        eyeLoc.setY(eyeLoc.getY());
        eyeLoc.setZ(eyeLoc.getZ());
        player.getWorld().spawn(eyeLoc, Fireball.class);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
