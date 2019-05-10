/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import com.sk89q.worldedit.blocks.BlockID;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryFX extends AbstractEffect {

    private int type, amount;

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
