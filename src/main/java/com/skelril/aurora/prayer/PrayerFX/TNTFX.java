package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class TNTFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.TNT;
    }

    @Override
    public void add(Player player) {

        Location playerLoc = player.getLocation();
        player.getWorld().createExplosion(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), .3F, false, false);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
