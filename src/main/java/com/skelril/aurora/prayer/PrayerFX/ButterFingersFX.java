package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class ButterFingersFX extends AbstractPrayer {

    @Override
    public PrayerType getType() {

        return PrayerType.BUTTERFINGERS;
    }

    @Override
    public void add(Player player) {

        for (ItemStack itemStack : player.getInventory().getArmorContents()) {
            if (itemStack != null) {
                player.getWorld().dropItem(player.getLocation(), itemStack);
            }
        }
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null) {
                player.getWorld().dropItem(player.getLocation(), itemStack);
            }
        }

        player.getInventory().setArmorContents(null);
        player.getInventory().clear();
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
