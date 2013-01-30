package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class ZombieFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.ZOMBIE;
    }

    @Override
    public void add(Player player) {

        if (player.getWorld().getEntitiesByClass(Zombie.class).size() < 1000) {
            player.getWorld().spawn(player.getLocation(), Zombie.class);
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
