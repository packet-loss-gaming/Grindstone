package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ArrowUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
