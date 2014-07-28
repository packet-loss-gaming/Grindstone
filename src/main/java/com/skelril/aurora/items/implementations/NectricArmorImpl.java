/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.prayer.Prayer;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerFX.NecrosisFX;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.extractor.entity.CombatantPair;
import com.skelril.aurora.util.extractor.entity.EDBEExtractor;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
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
