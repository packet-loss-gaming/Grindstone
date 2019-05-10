/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NecrosisFX extends AbstractEffect {

    private LivingEntity beneficiary = null;

    public void setBeneficiary(LivingEntity beneficiary) {
        this.beneficiary = beneficiary;
    }

    private boolean checkBeneficiary() {
        return beneficiary != null && beneficiary instanceof Player;
    }

    @Override
    public void add(Player player) {
        if (checkBeneficiary() && !PvPComponent.allowsPvP((Player) beneficiary, player)) return;
        EntityUtil.heal(beneficiary, 1);
        EntityUtil.forceDamage(player, 1);
    }

    @Override
    public PrayerType getType() {
        return PrayerType.NECROSIS;
    }
}
