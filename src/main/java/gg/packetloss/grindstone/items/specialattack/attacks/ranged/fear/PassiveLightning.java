/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.LocationAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

public class PassiveLightning extends LocationAttack implements RangedSpecial {
    private final Projectile projectile;

    public PassiveLightning(Projectile projectile, ItemStack usedItem) {
        super((LivingEntity) projectile.getShooter(), usedItem, projectile.getLocation());
        this.projectile = projectile;
    }

    @Override
    public void activate() {
        if (owner instanceof Player) {
            server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        // Simulate a lightning strike
        projectile.getWorld().strikeLightningEffect(projectile.getLocation());
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

            DamageUtil.damageWithSpecialAttack(owner, (LivingEntity) e, this, 5);
        }
    }
}
