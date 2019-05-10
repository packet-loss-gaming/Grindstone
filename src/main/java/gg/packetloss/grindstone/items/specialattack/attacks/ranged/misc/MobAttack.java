/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.misc;

import gg.packetloss.grindstone.items.specialattack.LocationAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.item.EffectUtil;
import org.bukkit.Location;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;

public class MobAttack extends LocationAttack implements RangedSpecial {

    private Class<? extends LivingEntity> type;

    public <T extends LivingEntity> MobAttack(LivingEntity owner, Location target, Class<T> type) {
        super(owner, target);
        this.type = type;
    }

    @Override
    public void activate() {

        EffectUtil.Strange.mobBarrage(target, type);

        if (Bat.class.equals(type)) {
            inform("Your bow releases a batty attack.");
        } else {
            inform("Your bow releases a " + type.getSimpleName().toLowerCase() + " attack.");
        }
    }
}
