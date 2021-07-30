/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.FreakyFour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.events.custom.item.ArmorBurstEvent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackSelectEvent;
import gg.packetloss.grindstone.events.entity.HallowCreeperEvent;
import gg.packetloss.grindstone.events.guild.NinjaSmokeBombEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.FearBlaze;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.SoulSmite;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.SoulReaper;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!parent.contains(parent.bossRegions.get(FreakyFourBoss.CHARLOTTE), player)) {
            return;
        }

        if (event.getBlock().getType() != Material.COBWEB) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!parent.contains(parent.bossRegions.get(FreakyFourBoss.CHARLOTTE), player)) {
            return;
        }

        event.setCancelled(true);
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


    private static final Set<Class<?>> SPECIAL_ATTACK_DENY_LIST = Set.of(
        Disarm.class,
        FearBlaze.class,
        SoulSmite.class,
        SoulReaper.class
    );

    @EventHandler(ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackSelectEvent event) {
        SpecialAttack attack = event.getSpec();
        if (!parent.contains(attack.getLocation())) {
            return;
        }

        Class<?> specClass = attack.getClass();
        if (SPECIAL_ATTACK_DENY_LIST.contains(specClass)) {
            event.tryAgain();
        }

        if (isAffectedBySpiderBite(event.getPlayer())) {
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

    private Map<FreakyFourBoss, Location> bossLastDamageLoc = new EnumMap<>(FreakyFourBoss.class);
    private Map<FreakyFourBoss, Integer> bossNumTimesDamaged = new EnumMap<>(FreakyFourBoss.class);

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

        if (entity instanceof Monster && damager instanceof Player player) {
            FreakyFourBoss boss = parent.getBossAtLocation(entity.getLocation());
            Location lastDamageLoc = bossLastDamageLoc.get(boss);
            if (lastDamageLoc != null && LocationUtil.isWithin2DDistance(entity.getLocation(), lastDamageLoc, 3)) {
                int totalTimesDamagedAtLoc = bossNumTimesDamaged.merge(boss, 1, Integer::sum);
                if (totalTimesDamagedAtLoc >= 3) {
                    server.getScheduler().runTaskLater(inst, () -> {
                        entity.teleport(player);
                        throwBack(entity);
                    }, 1);
                }
            } else {
                bossLastDamageLoc.put(boss, entity.getLocation());
                bossNumTimesDamaged.put(boss, 0);
            }
        }

        if (entity instanceof Creeper) {
            // Heal Da Bomb or teleport him randomly somewhere in the room
            if (healable.contains(event.getCause())) {
                event.setCancelled(true);
            } else {
                BlockVector3 damagerLoc = WorldEditBridge.toBlockVec3(damager.getLocation());
                final double minDist = Math.pow(parent.getConfig().daBombTeleMinDist, 2);
                entity.teleport(parent.getLocationInBossRoom(
                    FreakyFourBoss.DA_BOMB,
                    (vector) -> LocationUtil.distanceSquared2D(vector, damagerLoc) > minDist
                ));
            }
        } else if (entity instanceof Skeleton) {
            // Prevent melee damage to Snipee.
            //
            // When not running a special attack (as these show up without a projectile even if originally
            // caused by a projectile) don't check.
            if (SpecialAttackFactory.getCurrentSpecialAttack().isEmpty()) {
                if (damager instanceof Player && entity instanceof Skeleton && projectile == null) {
                    ChatUtil.sendNotice(damager, ChatColor.DARK_RED, "Melee can't harm me... Noob!");
                    event.setCancelled(true);
                }
            }
        } else if (entity instanceof Blaze) {
            if (damager instanceof Player && projectile != null) {
                ChatUtil.sendNotice(damager, ChatColor.DARK_RED, "Projectiles can't harm me... Mwahahaha!");
                event.setCancelled(true);
            }
        } else if (entity instanceof Player) {
            if (event.getCause().equals(DamageCause.LAVA)) {
                EntityUtil.forceDamage(
                        entity,
                        Math.max(
                                1,
                                ChanceUtil.getRandom(((LivingEntity) entity).getMaxHealth()) - 5
                        )
                );
            } else if (projectile != null) {
                if (entity.hasPermission("aurora.tome.divinity") && ChanceUtil.getChance(5)) {
                    ChatUtil.sendNotice((Player) entity, "A divine force deflects the projectile.");
                } else {
                    EntityUtil.forceDamage(entity, ((Player) entity).getMaxHealth() * parent.getConfig().snipeeDamage);
                }
                event.setCancelled(true);
            } else if (damager instanceof CaveSpider) {
                EntityUtil.heal(
                    parent.bossEntities.get(FreakyFourBoss.CHARLOTTE),
                    event.getDamage() * parent.getConfig().charlotteHealingScale
                );
            } else if (damager instanceof Spider) {
                parent.lastSpiderBite = System.currentTimeMillis();
                ChatUtil.sendWarning(entity, "You've been bit by Charlotte!");
                ChatUtil.sendWarning(entity, "Your equipment becomes less effective.");
            }
        }

        Bukkit.getScheduler().runTask(CommandBook.inst(), () -> {
            parent.updateBossBarProgress(parent.getBossAtLocation(entity.getLocation()));
        });
    }

    private boolean isAffectedBySpiderBite(Player player) {
        if (!parent.contains(parent.bossRegions.get(FreakyFourBoss.CHARLOTTE), player)) {
            return false;
        }

        return System.currentTimeMillis() - parent.lastSpiderBite < TimeUnit.SECONDS.toMillis(15);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorBurstEvent(ArmorBurstEvent event) {
        Player player = event.getPlayer();
        if (isAffectedBySpiderBite(player)) {
            event.setCancelled(true);
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
            ProtectedRegion daBombRegion = parent.bossRegions.get(FreakyFourBoss.DA_BOMB);

            BlockVector3 min = daBombRegion.getMinimumPoint();
            BlockVector3 max = daBombRegion.getMaximumPoint();

            int minX = min.getBlockX();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxZ = max.getBlockZ();

            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    if (ChanceUtil.getChance(12)) {
                        ExplosionStateFactory.createFakeExplosion(new Location(
                            parent.getWorld(),
                            x,
                            FreakyFourArea.GROUND_LEVEL,
                            z
                        ));
                    }
                }
            }

            for (Player player : parent.getContainedParticipantsIn(daBombRegion)) {
                player.setHealth(0);
            }

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
            switch (parent.getBossAtLocation(player.getLocation())) {
                case CHARLOTTE -> {
                    event.setDeathMessage(player.getName() + " died trying to escape a sticky situation");
                }
                case FRIMUS -> {
                    event.setDeathMessage(player.getName() + " went to the wrong barbeque");
                }
                case DA_BOMB -> {
                    event.setDeathMessage(player.getName() + " is now many little bitty pieces");
                }
                case SNIPEE -> {
                    event.setDeathMessage(player.getName() + " got snipeed");
                }
            }

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
