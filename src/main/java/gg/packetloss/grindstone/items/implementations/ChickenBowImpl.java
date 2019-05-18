/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.entity.ProjectileTickEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.misc.MobAttack;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class ChickenBowImpl extends AbstractItemFeatureImpl {
    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();
        Entity shooter = null;

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Entity) {
            shooter = (Entity) source;
        }

        if (shooter != null && shooter instanceof Player && projectile.hasMetadata("launcher")) {

            Object test = projectile.getMetadata("launcher").get(0).value();

            if (!(test instanceof ItemStack)) return;

            ItemStack launcher = (ItemStack) test;

            final Player owner = (Player) shooter;
            final Location targetLoc = projectile.getLocation();

            CustomItemSession session = getSession(owner);

            if (session.canSpec(SpecType.ANIMAL_BOW)) {
                Class<? extends LivingEntity> type = null;
                if (ItemUtil.isItem(launcher, CustomItems.CHICKEN_BOW)) {
                    type = Chicken.class;
                }

                if (type != null) {
                    SpecialAttackEvent specEvent = callSpec(owner, SpecType.ANIMAL_BOW, new MobAttack(owner, targetLoc, type));
                    if (!specEvent.isCancelled()) {
                        session.updateSpec(specEvent.getContext(), specEvent.getContextCoolDown());
                        specEvent.getSpec().activate();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArrowTick(ProjectileTickEvent event) {

        Projectile projectile = event.getEntity();
        Entity shooter = null;

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Entity) {
            shooter = (Entity) source;
        }

        if (shooter != null && shooter instanceof Player && projectile.hasMetadata("launcher")) {

            Object test = projectile.getMetadata("launcher").get(0).value();

            if (!(test instanceof ItemStack)) return;

            ItemStack launcher = (ItemStack) test;

            final Location location = projectile.getLocation();
            if (ItemUtil.isItem(launcher, CustomItems.CHICKEN_BOW)) {

                if (!ChanceUtil.getChance(5)) return;
                server.getScheduler().runTaskLater(inst, () -> {
                    final Chicken chicken = location.getWorld().spawn(location, Chicken.class);
                    chicken.setRemoveWhenFarAway(true);
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (chicken.isValid()) {
                            chicken.remove();
                            for (int i = 0; i < 20; i++) {
                                chicken.getWorld().playEffect(chicken.getLocation(), Effect.SMOKE, 0);
                            }
                        }
                    }, 20 * 3);
                }, 3);
            }
        }
    }

}
