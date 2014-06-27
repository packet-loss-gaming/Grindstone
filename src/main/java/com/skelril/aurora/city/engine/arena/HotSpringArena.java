/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import org.bukkit.Effect;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class HotSpringArena extends AbstractRegionedArena implements GenericArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    List<Player> playerL = new ArrayList<>();

    public HotSpringArena(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void run() {

        smoke();
        effect();
    }

    @Override
    public void disable() {

        // No disable code
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        // Do nothing
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.GENERIC;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {

        if (playerL.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    public void smoke() {

        try {
            if (!isEmpty()) {

                com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
                com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

                int minX = min.getBlockX();
                int minY = min.getBlockY();
                int minZ = min.getBlockZ();
                int maxX = max.getBlockX();
                int maxZ = max.getBlockZ();

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int sizeY = getWorld().getHighestBlockYAt(x, z) - minY;

                        for (int y = sizeY; y > 0; y--) {
                            Block block = getWorld().getBlockAt(x, y + minY, z);

                            if (EnvironmentUtil.isWater(block) && ChanceUtil.getChance(200)) {
                                getWorld().playEffect(block.getLocation(), Effect.ENDER_SIGNAL, 1);
                                if (getWorld().isThundering() && ChanceUtil.getChance(50)) {
                                    getWorld().spawn(block.getLocation(), Zombie.class);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

            log.warning("The region: " + getId() + " does not exists in the world: " + getWorld().getName() + ".");
        }
    }

    public void effect() {

        for (final Player player : getContained(Player.class)) {
            try {

                player.removePotionEffect(PotionEffectType.CONFUSION);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.removePotionEffect(PotionEffectType.POISON);
                player.removePotionEffect(PotionEffectType.SLOW);

                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.JUMP);

                int duration[] = new int[]{300, 180, 180};
                if (inst.hasPermission(player, "aurora.prayer.intervention")) {
                    for (int i = 0; i < duration.length; i++) {
                        duration[i] *= 2;
                    }
                    ChatUtil.sendNotice(player, "The gods double the effectiveness of the spring.");
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * duration[0], 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * duration[1], 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * duration[2], 1));

                Block downward = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                if (downward.getTypeId() != BlockID.LAPIS_LAZULI_BLOCK) {
                    downward = downward.getRelative(BlockFace.DOWN);
                    if (downward.getTypeId() != BlockID.LAPIS_LAZULI_BLOCK) {
                        continue;
                    }
                }

                // TODO Fix this

                playerL.add(player);

                //AntiCheatAPI.exemptPlayer(player, CheckType.WATER_WALK);
                //AntiCheatAPI.exemptPlayer(player, CheckType.FLY);
                //AntiCheatAPI.exemptPlayer(player, CheckType.SNEAK);

                IntegratedRunnable runnable = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {

                        if (player == null || !player.isValid()) return true;

                        Block downward = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (!BlockType.canPassThrough(downward.getTypeId()) && player.getLocation().getBlockY() > 70) {
                            return true;
                        }

                        player.setFlying(false);
                        player.setAllowFlight(false);

                        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                        Vector vector = player.getVelocity();
                        vector.add(new Vector(0, 1.2, 0));
                        player.setVelocity(vector);

                        player.setFallDistance(0);
                        return true;
                    }

                    @Override
                    public void end() {

                        if (playerL.contains(player)) {
                            playerL.remove(player);
                        }

                        if (player == null || !player.isValid()) return;

                        // AntiCheatAPI.unexemptPlayer(player, CheckType.WATER_WALK);
                        // AntiCheatAPI.unexemptPlayer(player, CheckType.FLY);
                        // AntiCheatAPI.unexemptPlayer(player, CheckType.SNEAK);
                    }
                };

                TimedRunnable timedRunnable = new TimedRunnable(runnable, 14);
                BukkitTask task = server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 15);
                timedRunnable.setTask(task);
            } catch (Exception e) {

                log.warning("The player: " + player.getName() + " was not boosted by the hot spring: " + getId() + ".");
            }
        }
    }
}
