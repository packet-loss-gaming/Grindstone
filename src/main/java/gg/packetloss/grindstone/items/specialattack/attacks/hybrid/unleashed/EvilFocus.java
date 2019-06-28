/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class EvilFocus extends EntityAttack implements MeleeSpecial, RangedSpecial {

    public EvilFocus(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    private int getNumRuns() {
        return (int) Math.max(3, Math.min(15, target.getHealth()));
    }

    private void lockPosition() {
        Location lockedLocation = target.getLocation();

        IntegratedRunnable freeze = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                if (owner.hasLineOfSight(target) && target.isValid()) {
                    lockedLocation.setDirection(target.getEyeLocation().getDirection()).setDirection(target.getEyeLocation().getDirection());
                    target.teleport(lockedLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                }

                return true;
            }

            @Override
            public void end() {
            }
        };

        TimedRunnable freezeTask = new TimedRunnable(freeze, getNumRuns());
        BukkitTask freezeTaskExecutor = server.getScheduler().runTaskTimer(inst, freezeTask, 20, 20);
        freezeTask.setTask(freezeTaskExecutor);
    }

    @Override
    public void activate() {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * (getNumRuns() + 3), 1), true);
        lockPosition();

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, .02F);
        inform("Your weapon traps your foe in their own sins.");
    }
}
