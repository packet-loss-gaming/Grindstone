package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.EnvironmentUtil;

/**
 * Author: Turtle9598
 */
public class SmokeFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        Location[] smoke = new Location[2];
        smoke[0] = player.getLocation();
        smoke[1] = player.getEyeLocation();
        EnvironmentUtil.generateRadialEffect(smoke, Effect.SMOKE);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
