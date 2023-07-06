/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.red;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ItemPointTranslator;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RedBlight extends EntityAttack implements MeleeSpecial, RangedSpecial {
    private static final int INTERVAL_TICKS = 10;

    private static final double SPREAD_DISTANCE = 2;
    private static final double SPREAD_DISTANCE_SQ = Math.pow(SPREAD_DISTANCE, 2);

    private static final ItemPointTranslator REDSTONE_CONVERTER = new ItemPointTranslator();

    static {
        REDSTONE_CONVERTER.addMapping(new ItemStack(Material.REDSTONE_BLOCK), 9);
        REDSTONE_CONVERTER.addMapping(new ItemStack(Material.REDSTONE), 1);
    }

    private final int numberOfRuns = ChanceUtil.getRangedRandom(16, 24);

    public RedBlight(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    public static boolean isApplicableTo(Player player) {
        InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);
        return REDSTONE_CONVERTER.calculateValue(adapter, false) > 0;
    }

    public Class<? extends Entity> getFilterType() {
        Class<? extends Entity> filterType = target.getClass();
        if (Monster.class.isAssignableFrom(filterType)) {
            filterType = Monster.class;
        }
        return filterType;
    }

    private boolean isCloseEnoughToBeInfected(List<Entity> infected, Entity entity) {
        Location sharedLoc = new Location(entity.getWorld(), 0, 0, 0);
        Location entityLoc = entity.getLocation();

        for (Entity anEntity : infected) {
            if (anEntity.getLocation(sharedLoc).distanceSquared(entityLoc) <= SPREAD_DISTANCE_SQ) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void activate() {
        final Location targeted = target.getLocation();

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(INTERVAL_TICKS);
        taskBuilder.setNumberOfRuns(numberOfRuns);

        Class<? extends Entity> filterType = getFilterType();

        Set<UUID> infectedIDs = new HashSet<>();
        infectedIDs.add(target.getUniqueId());
        List<Entity> infected = new ArrayList<>();
        infected.add(target);

        taskBuilder.setAction((times) -> {
            if (infected.isEmpty()) {
                return true;
            }

            // Find infectable entities
            for (Entity entity : targeted.getWorld().getEntitiesByClass(filterType)) {
                if (!entity.isValid() || entity.equals(owner)) {
                    continue;
                }

                UUID entityID = entity.getUniqueId();
                if (infectedIDs.contains(entityID)) {
                    continue;
                }

                // FIXME: Optimize this
                if (!isCloseEnoughToBeInfected(infected, entity)) {
                    continue;
                }

                infected.add(entity);
                infectedIDs.add(entityID);
            }

            // Spread out some of the load, the distance checks we just processed could get expensive.

            // Run the attack logic, and drain redstone - 1 tick from now
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                int redstoneTally = Integer.MAX_VALUE;

                InventoryAdapter adapter = null;
                if (owner instanceof Player) {
                    adapter = new PlayerStoragePriorityInventoryAdapter((Player) owner);

                    redstoneTally = REDSTONE_CONVERTER.calculateValue(adapter, true);
                    if (redstoneTally == 0) {
                        infected.clear();
                        infectedIDs.clear();
                        return;
                    }

                    CommandBook.callEvent(new RapidHitEvent((Player) owner));
                }

                for (Entity entity : infected) {
                    Location entityLoc = entity.getLocation();
                    for (int i = 0; i < 30; ++i) {
                        // note red can never be 0 for alternative colors
                        entityLoc.getWorld().spawnParticle(
                            Particle.REDSTONE,
                            entityLoc.clone().add(
                                ChanceUtil.getRangedRandom(-.5, .5),
                                ChanceUtil.getRangedRandom(0, .5),
                                ChanceUtil.getRangedRandom(-.5, .5)),
                            0, /*brightness=*/1,
                            /*red=*/0, /*green=*/0, /*blue=*/0,
                            new Particle.DustOptions(Color.RED, 1)
                        );
                    }

                    SpecialAttackFactory.processDamage(owner, (LivingEntity) entity, this, 10);

                    if (--redstoneTally == 0) {
                        break;
                    }
                }

                if (adapter != null) {
                    // Assign items, and update the remainder
                    redstoneTally = REDSTONE_CONVERTER.assignValue(adapter, redstoneTally);

                    // Update the player inventory
                    adapter.applyChanges();

                    // Drop any remaining redstone on the floor, if the inventory overflowed
                    World w = owner.getWorld();
                    REDSTONE_CONVERTER.streamValue(redstoneTally, (stack) -> {
                        w.dropItem(owner.getLocation(), stack);
                    });
                }
            }, 1);

            // Cleanup dead entities - 2 ticks from now
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                infected.removeIf(entity -> {
                    if (entity.isValid()) {
                        return false;
                    }

                    infectedIDs.remove(entity.getUniqueId());
                    return true;
                });
            }, 2);

            return true;
        });

        taskBuilder.build();

        inform("Your weapon unleashes the red blight.");
    }

    @Override
    public long getCoolDown(SpecType context) {
        return TimeUtil.convertTicksToMills(INTERVAL_TICKS * numberOfRuns);
    }
}
