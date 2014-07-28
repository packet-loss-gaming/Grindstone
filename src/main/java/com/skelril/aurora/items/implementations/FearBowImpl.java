/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.city.engine.combat.PvPComponent;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.items.CustomItemSession;
import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.items.generic.weapons.SpecWeaponImpl;
import com.skelril.aurora.items.specialattack.SpecType;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.fear.Curse;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.Disarm;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.FearBomb;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.FearStrike;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.MagicChain;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class FearBowImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, LivingEntity target) {
        switch (ChanceUtil.getRandom(5)) {
            case 1:
                Disarm disarmSpec = new Disarm(owner, target);
                if (disarmSpec.getItemStack() != null) {
                    return disarmSpec;
                }
            case 2:
                return new Curse(owner, target);
            case 3:
                return new MagicChain(owner, target);
            case 4:
                return new FearStrike(owner, target);
            case 5:
                return new FearBomb(owner, target);
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

            CustomItemSession session = getSession(owner);

            if (!session.canSpec(SpecType.RANGED)) {

                if (ItemUtil.isItem(launcher, CustomItems.FEAR_BOW)) {
                    if (!targetLoc.getWorld().isThundering() && targetLoc.getBlock().getLightFromSky() > 0) {

                        server.getPluginManager().callEvent(new RapidHitEvent(owner));

                        // Simulate a lightning strike
                        targetLoc.getWorld().strikeLightningEffect(targetLoc);
                        for (Entity e : projectile.getNearbyEntities(2, 4, 2)) {
                            if (!e.isValid() || !(e instanceof LivingEntity)) continue;
                            // Pig Zombie
                            if (e instanceof Pig) {
                                e.getWorld().spawn(e.getLocation(), PigZombie.class);
                                e.remove();
                                continue;
                            }
                            // Creeper
                            if (e instanceof Creeper) {
                                ((Creeper) e).setPowered(true);
                            }
                            // Player
                            if (e instanceof Player) {
                                if (!PvPComponent.allowsPvP(owner, (Player) e)) continue;
                            }

                            ((LivingEntity) e).damage(5, owner);
                        }
                    }
                }
            }
        }
    }
}
