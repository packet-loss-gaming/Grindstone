/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.support;

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
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Necrosis {
    private static EDBEExtractor<LivingEntity, Player, Projectile> necrosisExtractor = new EDBEExtractor<>(
      LivingEntity.class,
      Player.class,
      Projectile.class
    );

    private final PrayerComponent prayers;

    public Necrosis(PrayerComponent prayers) {
        this.prayers = prayers;
    }

    public void handleEvent(EntityDamageByEntityEvent event) {
        CombatantPair<LivingEntity, Player, Projectile> result = necrosisExtractor.extractFrom(event);

        if (result == null) return;

        Player defender = result.getDefender();
        if (ItemUtil.hasNectricArmour(defender) && ChanceUtil.getChance(4)) {
            LivingEntity attacker = result.getAttacker();
            if (attacker instanceof Player) {
                NecrosisFX necrosis = new NecrosisFX(defender);
                Prayer prayer = PrayerComponent.constructPrayer((Player) attacker, necrosis, 5000);
                prayers.influencePlayer((Player) attacker, prayer);
                defender.chat("Taste necrosis!");
            } else {
                EffectUtil.Necros.deathStrike(defender, Math.max(5, event.getDamage()));
            }
        }
    }
}
