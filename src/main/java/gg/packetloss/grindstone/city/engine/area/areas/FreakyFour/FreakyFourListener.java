/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.FreakyFour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.city.engine.combat.PvMComponent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.events.entity.HallowCreeperEvent;
import gg.packetloss.grindstone.events.guild.NinjaSmokeBombEvent;
import gg.packetloss.grindstone.state.player.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.HashSet;
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
        } else if (parent.contains(parent.frimus_RG, location)) {
            return parent.frimus_RG;
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
            event.getEntities().removeIf(next -> !region.equals(getRegion(next.getLocation())));
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
                // Teleport to Frimus
                if (parent.charlotte == null && parent.checkFrimus()) {
                    parent.cleanupFrimus();
                    parent.spawnFrimus();
                    player.teleport(new Location(parent.getWorld(), 374.5, 79, -304, 90, 0), TeleportCause.UNKNOWN);
                }
            } else if (parent.contains(parent.frimus_RG, player)) {
                // Teleport to Da Bomb
                if (parent.frimus == null && parent.checkDaBomb()) {
                    parent.cleanupDaBomb();
                    parent.spawnDaBomb();
                    player.teleport(new Location(parent.getWorld(), 350.5, 79, -304, 90, 0), TeleportCause.UNKNOWN);
                }
            } else if (parent.contains(parent.dabomb_RG, player)) {
                // Teleport to Snipee
                if (parent.daBomb == null && parent.checkSnipee()) {
                    parent.cleanupSnipee();
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
            if (parent.admin.isAdmin(player)) return;
            event.setTo(parent.entrance);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (event.isFlying() && parent.contains(player) && !parent.admin.isAdmin(player)) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, "You cannot fly here!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            player.teleport(parent.entrance);
        }
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
        } else if (entity instanceof Blaze) {
            if (damager instanceof Player && event.getCause().equals(DamageCause.PROJECTILE)) {
                ChatUtil.sendNotice(damager, ChatColor.DARK_RED, "Projectiles can't harm me... Mwahahaha!");
                event.setCancelled(true);
            }
        } else if (entity instanceof Player) {
            if (damager instanceof MagmaCube || event.getCause().equals(DamageCause.LAVA)) {
                EntityUtil.forceDamage(
                        entity,
                        Math.max(
                                1,
                                ChanceUtil.getRandom(((LivingEntity) entity).getHealth()) - 5
                        )
                );
            } else if (damager instanceof Creature) {
                if (projectile != null) {
                    if (entity.hasPermission("aurora.tome.divinity") && ChanceUtil.getChance(5)) {
                        ChatUtil.sendNotice((Player) entity, "A divine force deflects the arrow.");
                    } else {
                        EntityUtil.forceDamage(entity, ((Player) entity).getMaxHealth() * parent.getConfig().snipeeDamage);
                        entity.playEffect(EntityEffect.HURT);
                    }
                    event.setCancelled(true);
                } else if (damager instanceof CaveSpider) {
                    EntityUtil.heal(parent.charlotte, event.getDamage());
                }
            }
        }
        if (damager instanceof Player && entity instanceof LivingEntity) {
            PvMComponent.printHealth((Player) damager, (LivingEntity) entity);
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
                        ExplosionStateFactory.createExplosion(
                                new Location(parent.getWorld(), x, FreakyFourArea.groundLevel, z),
                                dmgFact,
                                false,
                                false
                        );
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
            } else if (e.equals(parent.frimus)) {
                parent.frimus = null;
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
        Player player = event.getEntity();
        if (parent.contains(player)) {
            try {
                parent.playerState.pushState(PlayerStateKind.FREAKY_FOUR, player);
                event.getDrops().clear();
            } catch (ConflictingPlayerStateException | IOException e) {
                e.printStackTrace();
            }

            parent.addSkull(player);
        }
    }
}
