/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear.Curse;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.FearBomb;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.FearStrike;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.MagicChain;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
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

      if (!(test instanceof ItemStack)) {
        return;
      }

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
              if (!e.isValid() || !(e instanceof LivingEntity)) {
                continue;
              }
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
                if (!PvPComponent.allowsPvP(owner, (Player) e)) {
                  continue;
                }
              }

              ((LivingEntity) e).damage(5, owner);
            }
          }
        }
      }
    }
  }
}
