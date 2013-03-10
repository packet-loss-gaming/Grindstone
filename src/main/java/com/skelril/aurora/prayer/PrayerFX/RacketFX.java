package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class RacketFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.RACKET;
    }

    @Override
    public void add(Player player) {

        player.playSound(player.getLocation(), Sound.ZOMBIE_WOODBREAK, 1, 0);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
