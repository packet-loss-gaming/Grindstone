/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.world.type.city.combat.PvPComponent;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NecrosisFX implements PassivePrayerEffect {
    private final LivingEntity beneficiary;

    public NecrosisFX(LivingEntity beneficiary) {
        Validate.notNull(beneficiary);

        this.beneficiary = beneficiary;
    }

    private boolean isBlockedPvP(Player player) {
        return beneficiary instanceof Player && !PvPComponent.allowsPvP((Player) beneficiary, player);
    }

    @Override
    public void trigger(Player player) {
        if (isBlockedPvP(player)) return;

        EntityUtil.heal(beneficiary, 1);
        EntityUtil.forceDamage(player, 1);

    }

    @Override
    public void strip(Player player) { }
}
