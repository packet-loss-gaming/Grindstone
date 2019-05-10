/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.prayer.Prayer;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerFX.NecrosisFX;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class NectricArmorImpl extends AbstractItemFeatureImpl {

    private static EDBEExtractor<LivingEntity, Player, Projectile> necrosisExtractor = new EDBEExtractor<>(
            LivingEntity.class,
            Player.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void necrosis(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, Player, Projectile> result = necrosisExtractor.extractFrom(event);

        if (result == null) return;

        Player defender = result.getDefender();
        if (ItemUtil.hasNectricArmour(defender) && ChanceUtil.getChance(4)) {
            LivingEntity attacker = result.getAttacker();
            if (attacker instanceof Player) {
                NecrosisFX necrosis = new NecrosisFX();
                necrosis.setBeneficiary(defender);
                Prayer prayer = PrayerComponent.constructPrayer((Player) attacker, necrosis, 5000);
                prayers.influencePlayer((Player) attacker, prayer);
                defender.chat("Taste necrosis!");
            } else {
                EffectUtil.Necros.deathStrike(defender, Math.max(5, event.getDamage()));
            }
        }
    }
}
