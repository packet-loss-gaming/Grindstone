package us.arrowcraft.aurora.prayer.PrayerFX;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.prayer.PrayerType;
import us.arrowcraft.aurora.util.ArrowUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;

import java.util.Collection;

/**
 * Author: Turtle9598
 */
public class DeadlyDefenseFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.DEADLYDEFENSE;
    }

    @Override
    public void add(Player player) {

        final Collection<Entity> entityList = player.getWorld().getEntitiesByClasses(EnvironmentUtil.hostileEntities());
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
