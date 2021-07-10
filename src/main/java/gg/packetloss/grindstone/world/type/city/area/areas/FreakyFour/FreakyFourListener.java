/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.FreakyFour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.events.entity.HallowCreeperEvent;
import gg.packetloss.grindstone.events.guild.NinjaSmokeBombEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.VectorUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.PlayerInventory;
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
        FreakyFourBoss boss = parent.getBossAtLocation(location);
        if (boss == null) {
            return null;
        }

        return parent.bossRegions.get(boss);
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

    private boolean checkSufficientEssence(Player player, FreakyFourBoss boss) {
        PlayerInventory pInv = player.getInventory();
        int numEssenceHeld = ItemUtil.countItemsOfName(pInv.getContents(), CustomItems.PHANTOM_ESSENCE.toString());
        if (numEssenceHeld < boss.getNumEssenceForCompletion()) {
            ChatUtil.sendError(player, "You don't have enough phantom essence to enter.");
            return false;
        }

        boolean removed = ItemUtil.removeItemOfName(
                player,
                CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE),
                boss.getNumEssenceForEntry(),
                false
        );
        Validate.isTrue(removed);

        return true;
    }

    private void tryAdvanceBossFight(Player player) {
        parent.validateBosses();

        FreakyFourBoss currentBoss = parent.getBossAtLocation(player.getLocation());
        // If fighting an active boss, fail.
        if (currentBoss != null && parent.checkActiveBoss(currentBoss)) {
            return;
        }

        // Figure out who the next boss is.
        FreakyFourBoss nextBoss = currentBoss == null ? FreakyFourBoss.CHARLOTTE : currentBoss.next();
        // If the next boss is no one, we're done return the player to the entrance
        if (nextBoss == null) {
            player.teleport(parent.entrance);
            return;
        }

        // Check that there are sufficient essence to power the transition
        if (!checkSufficientEssence(player, nextBoss)) {
            return;
        }

        // Check that the next boss is not active
        if (parent.checkActiveBoss(nextBoss)) {
            return;
        }

        // Advance to the next boss
        parent.cleanSpawn(nextBoss);
        player.teleport(parent.getEntrance(nextBoss), TeleportCause.UNKNOWN);
        parent.updateBossBars();
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        if (event.getHymn() != HymnSingEvent.Hymn.PHANTOM) {
            return;
        }

        Player player = event.getPlayer();
        if (!parent.contains(player)) {
            return;
        }

        tryAdvanceBossFight(player);
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
            if (event.getCause().equals(DamageCause.LAVA)) {
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
                    }
                    event.setCancelled(true);
                } else if (damager instanceof CaveSpider) {
                    EntityUtil.heal(parent.bossEntities.get(FreakyFourBoss.CHARLOTTE), event.getDamage());
                }
            }
        }

        parent.updateBossBarProgress(parent.getBossAtLocation(entity.getLocation()));
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
            ProtectedRegion daBombRegion = parent.bossRegions.get(FreakyFourBoss.DA_BOMB);

            BlockVector3 min = daBombRegion.getMinimumPoint();
            BlockVector3 max = daBombRegion.getMaximumPoint();

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
            for (FreakyFourBoss boss : FreakyFourBoss.values()) {
                if (e.equals(parent.bossEntities.get(boss))) {
                    parent.updateBossBars();
                    parent.bossEntities.put(boss, null);
                    parent.announceKill(boss);

                    if (boss == FreakyFourBoss.SNIPEE) {
                        Player killer = e.getKiller();
                        if (killer != null) {
                            parent.generateLootChest();
                        }
                    }
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
                event.setDroppedExp(0);
            } catch (ConflictingPlayerStateException | IOException e) {
                e.printStackTrace();
            }

            parent.addSkull(player);
        }
    }

    @EventHandler
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (event.getKind() != PlayerStateKind.FREAKY_FOUR_SPECTATOR) {
            return;
        }

        Player player = event.getPlayer();

        FreakyFourBoss targetBoss = parent.getFirstFullBossRoom().orElse(FreakyFourBoss.CHARLOTTE);
        Location targetLoc = parent.getEntrance(targetBoss);

        // Try to set the player looking at something interesting
        ProtectedRegion targetBossRegion = parent.bossRegions.get(targetBoss);
        parent.getContainedParticipantsIn(targetBossRegion).stream().findAny().ifPresent((p) -> {
            targetLoc.setDirection(VectorUtil.createDirectionalVector(targetLoc, p.getLocation()));
        });

        player.teleport(targetLoc, TeleportCause.UNKNOWN);
    }
}
