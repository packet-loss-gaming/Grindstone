/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.generic.weapons;

import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import org.bukkit.entity.LivingEntity;

public interface SpecWeaponImpl {
    public SpecialAttack getSpecial(LivingEntity owner, LivingEntity target);
}
