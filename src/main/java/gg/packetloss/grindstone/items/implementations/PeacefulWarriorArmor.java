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
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class PeacefulWarriorArmor extends AbstractItemFeatureImpl {
    private boolean isBuildableWorld(World world) {
        return managedWorld.is(ManagedWorldIsQuery.ANY_BUIDABLE, world);
    }

    private boolean isHiddenFromEntity(Entity entity) {
        if (ApocalypseHelper.checkEntity(entity)) {
            return true;
        }

        return isBuildableWorld(entity.getWorld()) && EntityUtil.isHostileMob(entity);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetEvent(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }

        if (isHiddenFromEntity(event.getEntity()) && ItemUtil.hasPeacefulWarriorArmor(target)) {
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

        if (ItemUtil.hasPeacefulWarriorArmor(attacker) && isHiddenFromEntity(defender)) {
            EntityUtil.forceDamage(attacker, 12);
        } else if (ItemUtil.hasPeacefulWarriorArmor(defender) && isHiddenFromEntity(attacker)) {
            event.setCancelled(true);

            // Clear the target if this is a monster
            if (attacker instanceof Mob monster) {
                monster.setTarget(null);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseEvent(ApocalypsePersonalSpawnEvent event) {
        if (ItemUtil.hasPeacefulWarriorArmor(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
