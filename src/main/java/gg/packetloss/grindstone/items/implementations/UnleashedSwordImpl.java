/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.EvilFocus;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.Speed;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed.DoomBlade;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed.HealingLight;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed.Regen;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class UnleashedSwordImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        switch (ChanceUtil.getRandom(6)) {
            case 1:
                return new EvilFocus(owner, usedItem, target);
            case 2:
                return new HealingLight(owner, usedItem, target);
            case 3:
                return new Speed(owner, usedItem, target);
            case 4:
                return new Regen(owner, usedItem, target);
            case 5:
                return new DoomBlade(owner, usedItem, target);
            case 6:
                return new LifeLeech(owner, usedItem, target);
        }
        return null;
    }
}
