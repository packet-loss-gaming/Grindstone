/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FearBomb extends EntityAttack implements RangedSpecial {

    public FearBomb(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        final List<Block> blocks = new ArrayList<>();
        Block block = target.getLocation().getBlock();
        blocks.add(block);
        for (BlockFace blockFace : EnvironmentUtil.getNearbyBlockFaces()) {
            blocks.add(block.getRelative(blockFace));
        }

        List<Block> blockList = new ArrayList<>();
        for (BlockFace blockFace : EnvironmentUtil.getNearbyBlockFaces()) {
            for (Block aBlock : blocks) {
                Block testBlock = aBlock.getRelative(blockFace);
                if (!blocks.contains(testBlock) && !blockList.contains(testBlock)) blockList.add(testBlock);
            }
        }

        blocks.addAll(blockList);

        final FearBomb spec = this;
        World world = target.getLocation().getWorld();
        Set<Location> changedLocations = new HashSet<>();
        IntegratedRunnable bomb = new IntegratedRunnable() {

            @Override
            public boolean run(int times) {
                List<Player> players = world.getPlayers();

                for (Block block : blocks) {
                    Location loc = block.getLocation();
                    changedLocations.add(loc);

                    World world = loc.getWorld();

                    while (loc.getY() > 0 && !world.getBlockAt(loc).getType().isSolid()) {
                        loc.add(0, -1, 0);
                    }

                    if (times % 2 == 0) {
                        for (Player player : players) {
                            if (!player.isValid()) continue;
                            player.sendBlockChange(loc, Material.WHITE_WOOL, (byte) 0);
                        }
                    } else {
                        for (Player player : players) {
                            if (!player.isValid()) continue;
                            player.sendBlockChange(loc, Material.RED_WOOL, (byte) 0);
                        }
                    }
                }
                return true;
            }

            @Override
            public void end() {
                if (owner instanceof Player) {
                    server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
                }

                Location loc = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                for (Block block : blocks) {
                    loc = block.getLocation(loc);
                    World world = loc.getWorld();

                    while (loc.getY() > 0 && !world.getBlockAt(loc).getType().isSolid()) {
                        loc.add(0, -1, 0);
                    }

                    ExplosionStateFactory.createFakeExplosion(loc);
                    for (LivingEntity entity : loc.getNearbyLivingEntities(2, 2, 2)) {
                        if (!entity.isValid()) continue;

                        DamageUtil.damageWithSpecialAttack(owner, entity, spec, entity instanceof Player ? 200 : 10000);
                    }
                }

                List<Player> players = world.getPlayers();
                for (Location changedLoc : changedLocations) {
                    BlockData blockData = changedLoc.getBlock().getBlockData();

                    for (Player player : players) {
                        player.sendBlockChange(changedLoc, blockData);
                    }
                }
            }
        };

        TimedRunnable timedRunnable = new TimedRunnable(bomb, 15);

        BukkitTask task = server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 5);
        timedRunnable.setTask(task);

        inform("Your bow creates a powerful bomb.");
    }
}
