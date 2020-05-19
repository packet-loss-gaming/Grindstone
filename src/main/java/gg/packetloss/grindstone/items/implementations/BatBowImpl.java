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
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import static gg.packetloss.grindstone.ProjectileWatchingComponent.getSpawningItem;

public class BatBowImpl extends AbstractItemFeatureImpl {
    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Player) {
            getSpawningItem(projectile).ifPresent((launcher) -> {
                final Player owner = (Player) source;
                final Location targetLoc = projectile.getLocation();

                CustomItemSession session = getSession(owner);

                if (session.canSpec(SpecType.ANIMAL_BOW)) {
                    Class<? extends LivingEntity> type = null;
                    if (ItemUtil.isItem(launcher, CustomItems.BAT_BOW)) {
                        type = Bat.class;
                    }

                    if (type != null) {
                        SpecialAttackEvent specEvent = callSpec(owner, SpecType.ANIMAL_BOW, new MobAttack(owner, launcher, targetLoc, type));
                        if (!specEvent.isCancelled()) {
                            session.updateSpec(specEvent.getContext(), specEvent.getContextCoolDown());
                            specEvent.getSpec().activate();
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    public void onArrowTick(ProjectileTickEvent event) {
        Projectile projectile = event.getEntity();

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Player) {
            getSpawningItem(projectile).ifPresent((launcher) -> {
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
            });
        }
    }
}
