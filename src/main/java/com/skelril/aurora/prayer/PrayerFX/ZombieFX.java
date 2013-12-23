package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

/**
 * Author: Turtle9598
 */
public class ZombieFX extends AbstractEffect {

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
