/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Disarm extends EntityAttack implements RangedSpecial {

    public Disarm(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        final ItemStack held = getItemStack();

        if (held == null) return;

        if (target instanceof Player) {

            PlayerInventory inv = ((Player) target).getInventory();

            ItemStack[] items = inv.getContents();

            int heldIndex = inv.getHeldItemSlot();
            int itemIndex;
            do {
                itemIndex = ChanceUtil.getRandom(9) - 1;
            } while (itemIndex == heldIndex);

            ItemStack previousItem = items[itemIndex];
            items[itemIndex] = items[heldIndex];
            items[heldIndex] = previousItem;

            inv.setContents(items);
        } else {
            target.getEquipment().setItemInMainHand(null);
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                if (target.isValid()) {
                    target.getEquipment().setItemInMainHand(held);
                }
            }, 20 * 3);
        }

        inform("Your bow disarms its victim.");
    }

    public ItemStack getItemStack() {

        ItemStack held;
        if (target instanceof Player) {
            held = ((Player) target).getItemInHand().clone();
        } else if (target instanceof Skeleton) {
            held = null;
        } else {
            held = target.getEquipment().getItemInHand().clone();
        }
        return ItemUtil.isNullItemStack(held) ? null : held;
    }
}
