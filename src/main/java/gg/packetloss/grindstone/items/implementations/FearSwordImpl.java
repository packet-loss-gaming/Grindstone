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
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.*;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class FearSwordImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        switch (ChanceUtil.getRandom(6)) {
            case 1:
                return new ChainLightning(owner, usedItem, target);
            case 2:
                return new FearBlaze(owner, usedItem, target);
            case 3:
                return new Curse(owner, usedItem, target);
            case 4:
                return new Weaken(owner, usedItem, target);
            case 5:
                return new Decimate(owner, usedItem, target);
            case 6:
                return new SoulSmite(owner, usedItem, target);
        }
        return null;
    }
}
