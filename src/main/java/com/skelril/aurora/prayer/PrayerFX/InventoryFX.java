package com.skelril.aurora.prayer.PrayerFX;

import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ChanceUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class InventoryFX extends AbstractPrayer {

    int type, amount;

    public InventoryFX() {
        super();
        this.type = BlockID.DIRT;
        this.amount = 1;
    }

    public InventoryFX(int type, int amount) {
        super();
        this.type = type;
        this.amount = amount;
    }

    @Override
    public PrayerType getType() {

        return PrayerType.INVENTORY;
    }

    @Override
    public void add(Player player) {

        ItemStack held = player.getItemInHand().clone();
        ItemStack stack = new ItemStack(type, ChanceUtil.getRandom(amount));
        if (held != null && held.getTypeId() != BlockID.AIR && !held.isSimilar(stack)) {
            Item item = player.getWorld().dropItem(player.getLocation(), held);
            item.setPickupDelay(20 * 5);
        }
        player.setItemInHand(stack);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
