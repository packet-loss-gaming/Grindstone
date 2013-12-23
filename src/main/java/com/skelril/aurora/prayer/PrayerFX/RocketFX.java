package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Author: Turtle9598
 */
public class RocketFX extends AbstractEffect {

    public RocketFX() {

    }

    @Override
    public PrayerType getType() {

        return PrayerType.ROCKET;
    }

    @Override
    public void add(Player player) {

        super.add(player);
        Location playerLoc = player.getLocation();
        if (playerLoc.getY() - 4 < player.getWorld().getHighestBlockYAt(playerLoc.getBlockX(), playerLoc.getBlockZ())) {
            player.setVelocity(new Vector(0, 4, 0));
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
