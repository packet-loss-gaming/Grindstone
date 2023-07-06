/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class Nightmare extends EntityAttack implements MeleeSpecial {
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
    private static final int NUM_SEED_POINTS = 100;

    private HashSet<Location> locations = new HashSet<>();

    public Nightmare(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);

        seedLocations();
    }

    private void seedLocations() {
        Location origin = target.getLocation().add(0, 5, 0);

        for (int i = 0; i < NUM_SEED_POINTS; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * 12;

            Location pt = origin.clone();
            pt.setX(origin.getX() + radius * Math.cos(angle));
            pt.setZ(origin.getZ() + radius * Math.sin(angle));

            locations.add(pt);
        }
    }

    public boolean isValid() {
        int solid = 0;

        for (Location location : locations) {
            if (location.getBlock().getType().isSolid()) {
                ++solid;
            }
        }

        return locations.size() - solid > (locations.size() / 2);
    }

    @Override
    public void activate() {
        inform("You unleash a nightmare upon the plane.");

        IntegratedRunnable hellFire = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                locations.stream().filter(location -> ChanceUtil.getChance(3)).forEach(location -> {
                    Snowball snowball = location.getWorld().spawn(location, Snowball.class);
                    snowball.setMetadata("rogue-snowball", new FixedMetadataValue(CommandBook.inst(), true));
                    snowball.setMetadata("nightmare", new FixedMetadataValue(CommandBook.inst(), true));
                    snowball.setShooter(owner);
                });
                return true;
            }

            @Override
            public void end() {
                inform("Your nightmare fades away...");
            }
        };

        TimedRunnable runnable = new TimedRunnable(hellFire, 40);
        runnable.setTask(Bukkit.getScheduler().runTaskTimer(CommandBook.inst(), runnable, 50, 10));
    }
}
