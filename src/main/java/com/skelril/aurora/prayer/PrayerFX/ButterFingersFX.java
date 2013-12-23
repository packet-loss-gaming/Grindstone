package com.skelril.aurora.prayer.PrayerFX;

import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class ButterFingersFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.BUTTERFINGERS;
    }

    @Override
    public void add(Player player) {

        for (ItemStack itemStack : player.getInventory().getArmorContents()) {
            if (itemStack != null && itemStack.getTypeId() != BlockID.AIR) {
                player.getWorld().dropItem(player.getLocation(), itemStack.clone());
            }
        }
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getTypeId() != BlockID.AIR) {
                player.getWorld().dropItem(player.getLocation(), itemStack.clone());
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
