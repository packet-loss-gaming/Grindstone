/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.FreakyFour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminState;
import com.skelril.aurora.city.engine.area.AreaListener;
import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.events.entity.HallowCreeperEvent;
import com.skelril.aurora.events.guild.NinjaSmokeBombEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EntityUtil;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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

    private ProtectedRegion getRegion(Location location) {
        if (parent.contains(parent.charlotte_RG, location)) {
            return parent.charlotte_RG;
        } else if (parent.contains(parent.magmacubed_RG, location)) {
            return parent.magmacubed_RG;
        } else if (parent.contains(parent.dabomb_RG, location)) {
            return parent.dabomb_RG;
        } else if (parent.contains(parent.snipee_RG, location)) {
            return parent.snipee_RG;
        } else {
            return null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNinjaBomb(NinjaSmokeBombEvent event) {
        Player player = event.getPlayer();
        ProtectedRegion region = getRegion(player.getLocation());
        if (region != null) {
            Iterator<Entity> it = event.getEntities().iterator();
            while (it.hasNext()) {
                Entity next = it.next();
                if (!region.equals(getRegion(next.getLocation()))) {
                    it.remove();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHallowCreeper(HallowCreeperEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        if (!event.getHymn().equals(HymnSingEvent.Hymn.PHANTOM)) return;
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            parent.validateBosses();
            if (parent.contains(parent.charlotte_RG, player)) {
                // Teleport to Magma Cubed
                if (parent.charlotte == null && parent.checkMagmaCubed()) {
                    parent.spawnMagmaCubed();
                    player.teleport(new Location(parent.getWorld(), 374.5, 79, -304, 90, 0), TeleportCause.UNKNOWN);
                }
            } else if (parent.contains(parent.magmacubed_RG, player)) {
                // Teleport to Da Bomb
                if (parent.magmaCubed.isEmpty() && parent.checkDaBomb()) {
                    parent.spawnDaBomb();
                    player.teleport(new Location(parent.getWorld(), 350.5, 79, -304, 90, 0), TeleportCause.UNKNOWN);
                }
            } else if (parent.contains(parent.dabomb_RG, player)) {
                // Teleport to Snipee
                if (parent.daBomb == null && parent.checkSnipee()) {
                    parent.spawnSnipee();
                    player.teleport(new Location(parent.getWorld(), 326.5, 79, -304, 90, 0), TeleportCause.UNKNOWN);
                }
            } else if (parent.contains(parent.snipee_RG, player)) {
                // Teleport to Spawn
                if (parent.snipee == null) {
                    player.teleport(parent.entrance);
                }
            } else if (parent.checkCharlotte()) {
                // Teleport to Charlotte
                parent.cleanupCharlotte();
                parent.spawnCharlotte();
                player.teleport(new Location(parent.getWorld(), 398.5, 79, -304, 90, 0), TeleportCause.UNKNOWN);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();

        if (parent.contains(to) && !event.getCause().equals(TeleportCause.UNKNOWN)) {
            Player player = event.getPlayer();
            if (parent.admin.isAdmin(player, AdminState.ADMIN)) return;
            event.setTo(parent.entrance);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            player.teleport(parent.entrance);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSlimeSplit(SlimeSplitEvent event) {
        Slime slime = event.getEntity();
        if (!parent.contains(slime)) return;
        event.setCount((int) Math.pow(event.getCount(), 2.5));
        server.getScheduler().runTaskLater(inst, () -> {
            for (MagmaCube cube : parent.getContained(MagmaCube.class)) {
                if (parent.magmaCubed.contains(cube)) continue;
                cube.setCustomName("Magma Cubed");
                double percentHealth = Math.max(1, Math.round(slime.getMaxHealth() * .75));
                cube.setMaxHealth(percentHealth);
                cube.setHealth(percentHealth);
                parent.magmaCubed.add(cube);
            }
        }, 1);
    }


    private static Set<DamageCause> healable = new HashSet<>();

    static {
        healable.add(DamageCause.BLOCK_EXPLOSION);
        healable.add(DamageCause.ENTITY_EXPLOSION);
        healable.add(DamageCause.WITHER);
    }


    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!parent.contains(entity)) return;
        Projectile projectile = null;
        Entity damager = null;
        if (event instanceof EntityDamageByEntityEvent) {
            damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Projectile) {
                projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Entity) {
                    damager = (Entity) projectile.getShooter();
                } else {
                    damager = null;
                }
            }
        }
        if (entity instanceof Creeper || entity instanceof Skeleton) {
            boolean backTeleport = projectile == null && ChanceUtil.getChance(parent.getConfig().backTeleport);
            if (healable.contains(event.getCause())) {
                EntityUtil.heal(entity, event.getDamage());
                event.setCancelled(true);
            } else if ((backTeleport || projectile != null) && damager != null) {
                double distSQ = 2;
                double maxDist = 1;
                if (entity instanceof Skeleton) {
                    distSQ = entity.getLocation().distanceSquared(damager.getLocation());
                    maxDist = parent.getConfig().snipeeTeleportDist;
                }
                if (backTeleport || distSQ > Math.pow(maxDist, 2)) {
                    final Entity finalDamager = damager;
                    server.getScheduler().runTaskLater(inst, () -> {
                        entity.teleport(finalDamager);
                        throwBack(entity);
                    }, 1);
                }
            }
        }
        if (entity instanceof Player && damager instanceof Creature) {
            if (projectile != null) {
                EntityUtil.forceDamage(entity, ((Player) entity).getMaxHealth() * parent.getConfig().snipeeDamage);
                entity.playEffect(EntityEffect.HURT);
                event.setCancelled(true);
            } else if (damager instanceof MagmaCube) {
                event.setDamage(event.getDamage() * parent.getConfig().magmaCubedDamageModifier);
            } else if (damager instanceof CaveSpider) {
                EntityUtil.heal(parent.charlotte, event.getDamage());
            }
        }
        if (damager instanceof Player) {
            final Entity finalDamager = damager;
            final int oldCurrent = (int) Math.ceil(((LivingEntity) entity).getHealth());

            server.getScheduler().runTaskLater(inst, () -> {

                int current = (int) Math.ceil(((LivingEntity) entity).getHealth());

                if (oldCurrent == current) return;

                int max = (int) Math.ceil(((LivingEntity) entity).getMaxHealth());

                String message;

                if (current > 0) {
                    message = ChatColor.DARK_AQUA + "Entity Health: " + current + " / " + max;
                } else {
                    message = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "KO!";
                }

                ChatUtil.sendNotice((Player) finalDamager, message);
            }, 1);
        }
    }

    private void throwBack(Entity entity) {
        Vector vel = entity.getLocation().getDirection();
        vel.multiply(-ChanceUtil.getRangedRandom(1.2, 1.5));
        vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
        entity.setVelocity(vel);
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
            int dmgFact = (int) (Math.max(3, (((Creeper) entity).getHealth() / ((Creeper) entity).getMaxHealth())
                    * parent.getConfig().daBombTNTStrength));
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    if (ChanceUtil.getChance(parent.getConfig().daBombTNT)) {
                        parent.getWorld().createExplosion(x, FreakyFourArea.groundLevel, z, dmgFact, false, false);
                    }
                }
            }
            throwBack(entity);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (parent.contains(event.getEntity())) {
            LivingEntity e = event.getEntity();
            if (e.equals(parent.charlotte)) {
                parent.charlotte = null;
            } else if (e instanceof MagmaCube && parent.magmaCubed.contains(e)) {
                parent.magmaCubed.remove(e);
            } else if (e.equals(parent.daBomb)) {
                parent.daBomb = null;
            } else if (e.equals(parent.snipee)) {
                parent.snipee = null;
                Player killer = e.getKiller();
                if (killer != null) {
                    double loot = parent.economy.getBalance(killer.getName()) * parent.getConfig().bankPercent;
                    loot = Math.max(loot, parent.getConfig().minLoot);
                    parent.economy.depositPlayer(killer.getName(), loot);
                    ChatUtil.sendNotice(killer, "The boss drops " + ChatColor.WHITE + parent.economy.format(loot));
                }
            }
            event.getDrops().clear();
            event.setDroppedExp(0);
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
            parent.addSkull(player);
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
