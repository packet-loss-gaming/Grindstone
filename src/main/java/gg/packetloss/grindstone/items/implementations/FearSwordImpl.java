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
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear.HellCano;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.*;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FearSwordImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        return ChanceUtil.supplyRandom(
            () -> new ChainLightning(owner, usedItem, target),
            () -> new FearBlaze(owner, usedItem, target),
            () -> new Curse(owner, usedItem, target),
            () -> new Weaken(owner, usedItem, target),
            () -> new Decimate(owner, usedItem, target),
            () -> new HellCano(owner, usedItem, target),
            () -> {
                if (target instanceof Player) {
                    return getSpecial(owner, usedItem, target);
                }

                // Affects Mobs Only
                return new SoulSmite(owner, usedItem, target);
            }
        );
    }
}
