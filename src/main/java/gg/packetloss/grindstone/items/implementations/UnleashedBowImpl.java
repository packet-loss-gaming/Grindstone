/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.EvilFocus;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.Speed;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Famine;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Surge;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import static gg.packetloss.grindstone.ProjectileWatchingComponent.getSpawningItem;

public class UnleashedBowImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        switch (ChanceUtil.getRandom(6)) {
            case 1:
                return new Famine(owner, usedItem, target);
            case 2:
                return new LifeLeech(owner, usedItem, target);
            case 3:
                return new EvilFocus(owner, usedItem, target);
            case 4:
                return new Speed(owner, usedItem, target);
            case 5:
                return new GlowingFog(owner, usedItem, target);
            case 6:
                return new Surge(owner, usedItem, target);
        }
        return null;
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Player) {
            getSpawningItem(projectile).ifPresent((launcher) -> {
                final Player owner = (Player) source;
                final Location targetLoc = projectile.getLocation();

                if (ItemUtil.isItem(launcher, CustomItems.UNLEASHED_BOW) && !projectile.hasMetadata("splashed")) {

                    projectile.setMetadata("splashed", new FixedMetadataValue(inst, true));

                    TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

                    taskBuilder.setInterval(10);
                    taskBuilder.setNumberOfRuns(3);

                    taskBuilder.setAction((times) -> {
                        EnvironmentUtil.generateRadialEffect(targetLoc, Effect.ENDER_SIGNAL);

                        targetLoc.getWorld().getEntitiesByClasses(Item.class).stream().filter(e -> e.isValid()
                                && e.getLocation().distanceSquared(targetLoc) <= 16).forEach(e -> {
                            e.teleport(owner);
                        });

                        return true;
                    });

                    taskBuilder.setFinishAction(() -> {
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
                    });

                    taskBuilder.build();
                }
            });
        }
    }
}
