package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.*;
import us.arrowcraft.aurora.util.ArrowUtil;

import java.util.Collection;

/**
 * Author: Turtle9598
 */
public class DeadlyDefenseFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        final Collection<Entity> entityList = player.getWorld().getEntitiesByClasses(Zombie.class, Skeleton.class,
                Creeper.class, Spider.class, Slime.class, Blaze.class, Witch.class);
        short arrow = 0;
        for (Entity entity : entityList) {

            if (arrow > 10) break;
            if (entity.getLocation().distanceSquared(player.getLocation()) < 8 * 8) {
                ArrowUtil.shootArrow(player, (LivingEntity) entity, 1.6F, 0F);
            }
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
