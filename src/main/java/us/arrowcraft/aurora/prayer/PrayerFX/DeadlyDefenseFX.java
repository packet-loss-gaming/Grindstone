package us.arrowcraft.aurora.prayer.PrayerFX;
import com.sk89q.commandbook.util.EntityUtil;
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

        short arrow = 0;
        for (Entity entity : player.getNearbyEntities(8, 3, 8)) {

            if (arrow > 10) break;
            if (EnvironmentUtil.isHostileEntity(entity)) {
                ArrowUtil.shootArrow(player, (LivingEntity) entity, 1.6F, 0F);
            }
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
