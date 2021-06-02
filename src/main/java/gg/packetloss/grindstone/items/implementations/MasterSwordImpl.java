/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear.Curse;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class MasterSwordImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        switch (ChanceUtil.getRandom(2)) {
            case 1:
                return new Curse(owner, usedItem, target);
            case 2:
                return new LifeLeech(owner, usedItem, target);
        }
        return null;
    }
}
