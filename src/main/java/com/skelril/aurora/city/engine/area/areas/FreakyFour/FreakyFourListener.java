/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.FreakyFour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.skelril.aurora.admin.AdminState;
import com.skelril.aurora.city.engine.area.AreaListener;
import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EntityUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.logging.Logger;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class FreakyFourListener extends AreaListener<FreakyFourArea> {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public FreakyFourListener(FreakyFourArea parent) {
        super(parent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        if (!event.getHymn().equals(HymnSingEvent.Hymn.PHANTOM)) return;
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            parent.validateBosses();
            if (LocationUtil.isInRegion(parent.charlotte_RG, player)) {
                // Teleport to Magma Cubed
                if (parent.charolette == null && parent.checkMagmaCubed()) {
                    parent.spawnMagmaCubed();
                    player.teleport(new Location(parent.getWorld(), 374.5, 79, -304), TeleportCause.UNKNOWN);
                }
            } else if (LocationUtil.isInRegion(parent.magmacubed_RG, player)) {
                // Teleport to Da Bomb
                if (parent.magmaCubed.isEmpty() && parent.checkDaBomb()) {
                    parent.spawnDaBomb();
                    player.teleport(new Location(parent.getWorld(), 350.5, 79, -304), TeleportCause.UNKNOWN);
                }
            } else if (LocationUtil.isInRegion(parent.dabomb_RG, player)) {
                // Teleport to Snipee
                if (parent.daBomb == null && parent.checkSnipee()) {
                    parent.spawnSnipee();
                    player.teleport(new Location(parent.getWorld(), 326.5, 79, -304), TeleportCause.UNKNOWN);
                }
            } else if (LocationUtil.isInRegion(parent.snipee_RG, player)) {
                // Teleport to Spawn
                if (parent.snipee == null) {
                    player.teleport(parent.getWorld().getSpawnLocation());
                }
            } else if (parent.checkCharlotte()) {
                // Teleport to Charlotte
                parent.spawnCharlotte();
                player.teleport(new Location(parent.getWorld(), 398.5, 79, -304), TeleportCause.UNKNOWN);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (parent.contains(to) && !parent.contains(from) && !event.getCause().equals(TeleportCause.UNKNOWN)) {
            Player player = event.getPlayer();
            if (parent.admin.isAdmin(player, AdminState.ADMIN)) return;
            event.setCancelled(true);
            ChatUtil.sendError(player, "Your teleport is too weak to enter that area.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSlimeSplit(SlimeSplitEvent event) {
        Slime slime = event.getEntity();
        if (!parent.contains(slime)) return;
        event.setCount((int) Math.pow(event.getCount(), 3));
        server.getScheduler().runTaskLater(inst, () -> {
            for (MagmaCube cube : parent.getContained(MagmaCube.class)) {
                cube.setCustomName("Magma Cubed");
                cube.setMaxHealth(parent.getConfig().magmaCubedHP);
                cube.setHealth(parent.getConfig().magmaCubedHP);
                parent.magmaCubed.add(cube);
            }
        }, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!parent.contains(entity)) return;
        if (entity instanceof Creeper) {
            if (event.getCause().equals(DamageCause.BLOCK_EXPLOSION)) {
                EntityUtil.heal(entity, event.getDamage());
                event.setCancelled(true);
            } else if (event instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                if (damager instanceof Projectile) {
                    ProjectileSource source = ((Projectile) damager).getShooter();
                    if (source instanceof Entity) {
                        entity.teleport((Entity) source);
                    }
                }
            }
        } else if (entity instanceof Player && event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Projectile) {
                EntityUtil.forceDamage(entity, ((Player) entity).getMaxHealth() * parent.getConfig().snipeeDamage);
                entity.playEffect(EntityEffect.HURT);
                event.setCancelled(true);
            } else if (damager instanceof MagmaCube) {
                event.setDamage(event.getDamage() * parent.getConfig().magmaCubedDamageModifier);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreeperExplode(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (!parent.contains(entity)) return;
        if (entity instanceof Creeper) {
            BlockVector min = parent.dabomb_RG.getMinimumPoint();
            BlockVector max = parent.dabomb_RG.getMaximumPoint();
            int minX = min.getBlockX();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxZ = max.getBlockZ();
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    if (ChanceUtil.getChance(parent.getConfig().daBombTNT)) {
                        Location target = new Location(parent.getWorld(), x, 83, z);
                        if (target.getBlock().getType().isSolid()) continue;
                        parent.getWorld().spawn(target, TNTPrimed.class);
                    }
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (parent.contains(event.getEntity())) {
            LivingEntity e = event.getEntity();
            if (e.equals(parent.charolette)) {
                parent.charolette = null;
            } else if (e instanceof MagmaCube && parent.magmaCubed.contains(e)) {
                parent.magmaCubed.remove(e);
            } else if (e.equals(parent.daBomb)) {
                parent.daBomb = null;
            } else if (e.equals(parent.snipee)) {
                parent.snipee = null;
                // TOOD Reward code
                ChatUtil.sendNotice(parent.getContained(Player.class), "Yippee you win!");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        HashMap<String, PlayerState> playerState = parent.playerState;
        Player player = event.getEntity();
        if (parent.contains(player) && !parent.admin.isAdmin(player) && !playerState.containsKey(player.getName())) {
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        HashMap<String, PlayerState> playerState = parent.playerState;

        Player player = event.getPlayer();
        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !parent.admin.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playerState.remove(player.getName());
            }
        }
    }
}
