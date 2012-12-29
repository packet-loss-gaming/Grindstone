package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class ThrownFireballFX extends AbstractPrayer implements Throwable {

    @Override
    public void trigger(Player player) {

        Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2))
                .toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
        player.getWorld().spawn(loc, Fireball.class);
    }
}
