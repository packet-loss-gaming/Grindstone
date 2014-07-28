/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.skelril.aurora.items.CustomItemSession;
import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.items.specialattack.SpecType;
import com.skelril.aurora.items.specialattack.attacks.ranged.misc.MobAttack;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class BatBowImpl extends AbstractItemFeatureImpl {
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
                if (ItemUtil.isItem(launcher, CustomItems.BAT_BOW)) {
                    type = Bat.class;
                }

                if (type != null) {
                    SpecialAttackEvent specEvent = callSpec(owner, SpecType.RANGED, new MobAttack(owner, targetLoc, type));
                    if (!specEvent.isCancelled()) {
                        session.updateSpec(SpecType.ANIMAL_BOW, specEvent.getSpec().getCoolDown());
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
            if (ItemUtil.isItem(launcher, CustomItems.BAT_BOW)) {

                if (!ChanceUtil.getChance(5)) return;
                server.getScheduler().runTaskLater(inst, () -> {
                    final Bat bat = location.getWorld().spawn(location, Bat.class);
                    bat.setRemoveWhenFarAway(true);
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (bat.isValid()) {
                            bat.remove();
                            for (int i = 0; i < 20; i++) {
                                bat.getWorld().playEffect(bat.getLocation(), Effect.SMOKE, 0);
                            }
                        }
                    }, 20 * 3);
                }, 3);
            }
        }
    }
}
