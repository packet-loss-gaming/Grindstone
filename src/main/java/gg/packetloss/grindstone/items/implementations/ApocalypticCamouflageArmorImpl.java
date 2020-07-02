/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.apocalypse.ApocalypseHelper;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class ApocalypticCamouflageArmorImpl extends AbstractItemFeatureImpl {
    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetEvent(EntityTargetLivingEntityEvent event) {
        if (ApocalypseHelper.checkEntity(event.getEntity()) && ItemUtil.hasApocalypticCamouflage(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    private static final EDBEExtractor<LivingEntity, LivingEntity, Projectile> EXTRACTOR = new EDBEExtractor<>(
            LivingEntity.class,
            LivingEntity.class,
            Projectile.class
    );

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        CombatantPair<LivingEntity, LivingEntity, Projectile> result = EXTRACTOR.extractFrom(event);
        if (result == null) return;

        LivingEntity attacker = result.getAttacker();
        LivingEntity defender = result.getDefender();

        if (ItemUtil.hasApocalypticCamouflage(attacker) && ApocalypseHelper.checkEntity(defender)) {
            EntityUtil.forceDamage(attacker, 12);
        } else if (ItemUtil.hasApocalypticCamouflage(defender) && ApocalypseHelper.checkEntity(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseEvent(ApocalypsePersonalSpawnEvent event) {
        if (ItemUtil.hasApocalypticCamouflage(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
