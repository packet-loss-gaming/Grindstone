/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractXPArmor;
import com.skelril.aurora.util.item.ItemUtil;
import org.bukkit.entity.Player;

public class AncientArmorImpl extends AbstractXPArmor {
    @Override
    public boolean hasArmor(Player player) {
        return ItemUtil.hasAncientArmour(player);
    }
}
