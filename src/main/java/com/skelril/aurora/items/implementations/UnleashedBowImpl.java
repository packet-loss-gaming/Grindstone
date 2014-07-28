/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.city.engine.combat.PvPComponent;
import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.items.generic.weapons.SpecWeaponImpl;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.EvilFocus;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.Speed;
import com.skelril.aurora.items.specialattack.attacks.ranged.unleashed.Famine;
import com.skelril.aurora.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

public class UnleashedBowImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, LivingEntity target) {
        switch (ChanceUtil.getRandom(5)) {
            case 1:
                return new Famine(owner, target);
            case 2:
                return new LifeLeech(owner, target);
            case 3:
                return new EvilFocus(owner, target);
            case 4:
                return new Speed(owner, target);
            case 5:
                return new GlowingFog(owner, target);
        }
        return null;
    }

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

            if (ItemUtil.isItem(launcher, CustomItems.UNLEASHED_BOW) && !projectile.hasMetadata("splashed")) {

                projectile.setMetadata("splashed", new FixedMetadataValue(inst, true));

                IntegratedRunnable vacuum = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {

                        EnvironmentUtil.generateRadialEffect(targetLoc, Effect.ENDER_SIGNAL);

                        targetLoc.getWorld().getEntitiesByClasses(Item.class).stream().filter(e -> e.isValid()
                                && e.getLocation().distanceSquared(targetLoc) <= 16).forEach(e -> {
                            e.teleport(owner);
                        });
                        return true;
                    }

                    @Override
                    public void end() {

                        EnvironmentUtil.generateRadialEffect(targetLoc, Effect.ENDER_SIGNAL);

                        for (Entity e : targetLoc.getWorld().getEntitiesByClasses(Monster.class, Player.class)) {
                            if (!e.isValid() || e.equals(owner)) continue;
                            if (e.getLocation().distanceSquared(targetLoc) <= 16) {
                                if (e instanceof Item) {
                                    e.teleport(owner);
                                    continue;
                                }
                                if (e instanceof Player) {
                                    if (!PvPComponent.allowsPvP(owner, (Player) e)) continue;
                                }
                                e.setFireTicks(20 * 4);
                            }
                        }
                    }
                };
                TimedRunnable runnable = new TimedRunnable(vacuum, 3);
                runnable.setTask(server.getScheduler().runTaskTimer(inst, runnable, 1, 10));
            }
        }
    }
}
