package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Author: Turtle9598
 */
public class ThrownFireballFX extends AbstractTriggeredPrayer {

    public ThrownFireballFX() {

        super(PlayerInteractEvent.class);
    }

    @Override
    public void trigger(Player player) {

        Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2))
                .toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
        player.getWorld().spawn(loc, Fireball.class);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.FIREBALL;
    }
}
