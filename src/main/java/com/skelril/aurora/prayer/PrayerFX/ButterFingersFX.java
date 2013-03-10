package com.skelril.aurora.prayer.PrayerFX;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
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

        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null) {
                Item i = player.getWorld().dropItemNaturally(player.getLocation(), itemStack);

                PlayerDropItemEvent event = new PlayerDropItemEvent(player, i);
                CommandBook.callEvent(event);

                if (!event.isCancelled()) player.getInventory().remove(itemStack);
                else i.remove();
            }
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
