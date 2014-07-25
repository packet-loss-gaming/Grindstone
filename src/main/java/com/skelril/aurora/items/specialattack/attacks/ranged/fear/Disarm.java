/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.ranged.fear;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import com.skelril.aurora.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;

public class Disarm extends EntityAttack implements RangedSpecial {

    public Disarm(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        final ItemStack held = getItemStack();

        if (held == null) return;

        if (target instanceof Player) {

            ItemStack[] items = ((Player) target).getInventory().getContents();

            int heldS = ((Player) target).getInventory().getHeldItemSlot();
            int k = ChanceUtil.getRandom(items.length) - 1;

            items[heldS] = items[k];
            items[k] = held;

            ((Player) target).getInventory().setContents(items);
        } else {
            target.getEquipment().setItemInHand(null);
            server.getScheduler().runTaskLater(inst, () -> {
                if (target.isValid()) {
                    target.getEquipment().setItemInHand(held);
                }
            }, 20 * 3);
        }

        inform("Your bow disarms its victim.");
    }

    public ItemStack getItemStack() {

        ItemStack held;
        if (target instanceof Player) {
            held = ((Player) target).getItemInHand();
            if (held != null) held = held.clone();
        } else if (target instanceof Skeleton) {
            held = null;
        } else {
            held = target.getEquipment().getItemInHand();
            if (held != null) held = held.clone();
        }
        return held == null || held.getTypeId() == 0 ? null : held;
    }
}
