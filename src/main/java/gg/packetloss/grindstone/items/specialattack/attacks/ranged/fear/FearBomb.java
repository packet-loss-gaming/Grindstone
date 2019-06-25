/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ClothColor;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
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

                    while (loc.getY() > 0 && BlockType.canPassThrough(world.getBlockTypeIdAt(loc))) {
                        loc.add(0, -1, 0);
                    }

                    if (times % 2 == 0) {
                        for (Player player : players) {
                            if (!player.isValid()) continue;
                            player.sendBlockChange(loc, BlockID.CLOTH, (byte) ClothColor.WHITE.getID());
                        }
                    } else {
                        for (Player player : players) {
                            if (!player.isValid()) continue;
                            player.sendBlockChange(loc, BlockID.CLOTH, (byte) ClothColor.RED.getID());
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

                    while (loc.getY() > 0 && BlockType.canPassThrough(world.getBlockTypeIdAt(loc))) {
                        loc.add(0, -1, 0);
                    }

                    ExplosionStateFactory.createFakeExplosion(loc);
                    for (Entity entity : block.getWorld().getEntitiesByClasses(Monster.class, Player.class)) {
                        if (!entity.isValid()) continue;
                        if (entity.getLocation().distanceSquared(loc) <= 4) {
                            DamageUtil.damageWithSpecialAttack(owner, target, spec, entity instanceof Player ? 200 : 10000);
                        }
                    }
                }

                List<Player> players = world.getPlayers();
                for (Location changedLoc : changedLocations) {
                    Material type = changedLoc.getBlock().getType();
                    byte data = changedLoc.getBlock().getData();

                    for (Player player : players) {
                        player.sendBlockChange(changedLoc, type, data);
                    }
                }
            }
        };

        TimedRunnable timedRunnable = new TimedRunnable(bomb, 6);

        BukkitTask task = server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 20);
        timedRunnable.setTask(task);

        inform("Your bow creates a powerful bomb.");
    }
}
