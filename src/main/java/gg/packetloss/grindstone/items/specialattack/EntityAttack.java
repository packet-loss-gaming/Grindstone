/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public abstract class EntityAttack extends SpecialAttack {

    protected LivingEntity target;

    public EntityAttack(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem);
        this.target = target;
    }

    @Override
    public LivingEntity getTarget() {

        return target;
    }

    @Override
    public Location getLocation() {

        return target.getLocation();
    }
}
