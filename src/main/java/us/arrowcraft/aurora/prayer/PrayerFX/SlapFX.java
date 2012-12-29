package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import us.arrowcraft.aurora.util.ChatUtil;

import java.util.Random;

/**
 * Author: Turtle9598
 */
public class SlapFX extends AbstractPrayer {

    private final Random random = new Random();
    private static final AbstractPrayer[] subFX = new AbstractPrayer[] {
            new ButterFingersFX()
    };

    public SlapFX() {

        super(subFX);
    }

    @Override
    public void add(Player player) {

        super.add(player);
        Location playerLoc = player.getLocation();
        if (playerLoc.getY() - 4 < player.getWorld().getHighestBlockYAt(playerLoc.getBlockX(), playerLoc.getBlockZ())) {
            player.setVelocity(new Vector(
                    random.nextDouble() * 5.0 - 2.5,
                    random.nextDouble() * 4,
                    random.nextDouble() * 5.0 - 2.5));
            ChatUtil.sendWarning(player, "SLAP!");
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
