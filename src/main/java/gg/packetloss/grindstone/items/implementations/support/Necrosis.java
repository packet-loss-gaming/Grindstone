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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.function.Predicate;

public class Necrosis {
    private static EDBEExtractor<LivingEntity, Player, Projectile> necrosisExtractor = new EDBEExtractor<>(
      LivingEntity.class,
      Player.class,
      Projectile.class
    );

    private final PrayerComponent prayers;
    private final Predicate<Player> hasArmor;

    public Necrosis(PrayerComponent prayers, Predicate<Player> hasArmor) {
        this.prayers = prayers;
        this.hasArmor = hasArmor;
    }

    private Prayer constructNecrosisPrayer(Player attacker, Player defender) {
        NecrosisFX necrosis = new NecrosisFX(defender);
        int duration = ChanceUtil.getRangedRandom(3000, 6000);
        return PrayerComponent.constructPrayer(attacker, necrosis, duration);
    }

    private int getMaxUnits() {
        return 20;
    }

    private int getScaledHealthUnitsLost(Player defender) {
        double healthPercentage = (defender.getMaxHealth() - defender.getHealth()) / defender.getMaxHealth();
        return (int) (healthPercentage * getMaxUnits());
    }

    private void handlePlayer(Player attacker, Player defender, double damage) {
        int healthLost = getScaledHealthUnitsLost(defender);

        int healthUnits = healthLost / 3;
        int damageUnits = (int) (damage / 10);

        int executingUnits = healthUnits + damageUnits;

        if (!ChanceUtil.getChance(Math.max(4, getMaxUnits() - executingUnits))) {
            return;
        }

        prayers.influencePlayer(attacker, constructNecrosisPrayer(attacker, defender));
        defender.chat("Taste necrosis!");
    }

    private void handleEntity(LivingEntity defender, double damage) {
        if (!ChanceUtil.getChance(5)) {
            return;
        }

        EffectUtil.Necros.deathStrike(defender, Math.max(5, damage));
    }

    public void handleEvent(EntityDamageByEntityEvent event) {
        CombatantPair<LivingEntity, Player, Projectile> result = necrosisExtractor.extractFrom(event);

        if (result == null) return;

        Player defender = result.getDefender();
        if (hasArmor.test(defender)) {
            LivingEntity attacker = result.getAttacker();
            if (attacker instanceof Player) {
                handlePlayer((Player) attacker, defender, event.getDamage());
            } else {
                handleEntity(defender, event.getDamage());
            }
        }
    }
}
