/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.GiantBoss;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackSelectEvent;
import gg.packetloss.grindstone.events.environment.CreepSpeakEvent;
import gg.packetloss.grindstone.events.guild.NinjaSmokeBombEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.highscore.ScoreType;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.Decimate;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.SoulSmite;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed.DoomBlade;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.FearBomb;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.guild.ninja.Ignition;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.misc.MobAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Famine;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Surge;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.dropttable.BoundDropSpawner;
import gg.packetloss.grindstone.util.dropttable.MassBossKillInfo;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class GiantBossListener extends AreaListener<GiantBossArea> {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public GiantBossListener(GiantBossArea parent) {
        super(parent);
    }

    private static final List<PlayerTeleportEvent.TeleportCause> causes = new ArrayList<>(2);

    static {
        causes.add(PlayerTeleportEvent.TeleportCause.COMMAND);
        causes.add(PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (parent.contains(event.getTo(), 1) && causes.contains(event.getCause())) {
            for (PotionEffectType potionEffectType : PotionEffectType.values()) {
                if (potionEffectType == null) continue;
                if (player.hasPotionEffect(potionEffectType)) player.removePotionEffect(potionEffectType);
            }
        }

        if (parent.contains(event.getFrom()) && !parent.contains(event.getTo()) && parent.isParticipant(player)) {
            parent.handlePlayerSurrender();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {
        if (parent.contains(event.getPlayer()) && event.getCause().getEffect().getType().isHoly()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {
        if (parent.contains(event.getPlayer(), 1) || parent.contains(event.getTargeter(), 1)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player, 1)) {
            ItemStack stack = event.getItem();
            if (stack.getItemMeta() instanceof PotionMeta) {
                PotionMeta pMeta = (PotionMeta) stack.getItemMeta();
                if (pMeta.hasCustomEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    ChatUtil.sendWarning(player, "You find yourself unable to drink the potion.");
                    event.setCancelled(true);
                }
            }
        }
    }

    private static Set<Class<?>> generalBlacklistedSpecs = new HashSet<>();
    private static Set<Class<?>> bossBlacklistedSpecs = new HashSet<>();
    private static Set<Class<?>> ultimateBlacklistedSpecs = new HashSet<>();

    static {
        generalBlacklistedSpecs.add(GlowingFog.class);
        generalBlacklistedSpecs.add(Ignition.class);
        generalBlacklistedSpecs.add(Nightmare.class);
        generalBlacklistedSpecs.add(Disarm.class);
        generalBlacklistedSpecs.add(MobAttack.class);
        generalBlacklistedSpecs.add(FearBomb.class);

        bossBlacklistedSpecs.add(Famine.class);
        bossBlacklistedSpecs.add(SoulSmite.class);

        ultimateBlacklistedSpecs.add(Surge.class);
        ultimateBlacklistedSpecs.add(Decimate.class);
        ultimateBlacklistedSpecs.add(DoomBlade.class);
    }

    private long lastUltimateAttack = 0;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackSelectEvent event) {
        SpecialAttack attack = event.getSpec();
        if (!parent.contains(attack.getLocation())) return;

        Class<?> specClass = attack.getClass();
        LivingEntity target = attack.getTarget();

        if (target instanceof Giant) {
            if (bossBlacklistedSpecs.contains(specClass)) {
                event.tryAgain();
                return;
            }
            if (ultimateBlacklistedSpecs.contains(specClass)) {
                if (lastUltimateAttack == 0) {
                    lastUltimateAttack = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastUltimateAttack >= 15000) {
                    lastUltimateAttack = System.currentTimeMillis();
                } else {
                    event.tryAgain();
                    return;
                }
            }
        }

        if (generalBlacklistedSpecs.contains(specClass)) {
            event.tryAgain();
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {
        SpecialAttack attack = event.getSpec();
        if (!parent.contains(attack.getLocation())) return;

        if (ItemUtil.isInItemFamily(attack.getUsedItem(), ItemFamily.MASTER)) {
            event.setContextCooldown(event.getContext().getDelay() / 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {
        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        if (parent.boss != null && action.equals(Action.PHYSICAL) && parent.contains(player, 1)) {
            switch (block.getType()) {
                case STONE_PRESSURE_PLATE:
                    ProtectedRegion door;
                    if (LocationUtil.isInRegion(parent.eastDoor, block.getRelative(BlockFace.WEST).getLocation())) {
                        door = parent.eastDoor;
                    } else {
                        door = parent.westDoor;
                    }
                    parent.setDoor(door, Material.AIR);
                    server.getScheduler().runTaskLater(inst, () -> parent.setDoor(door, Material.CHISELED_SANDSTONE), 20 * 10);
                    break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }

        Entity entity = event.getEntity();
        if (parent.contains(entity) && !parent.contains(target)) {
            event.setCancelled(true);
        }
    }

    private static Set<EntityDamageEvent.DamageCause> meleeReasons = new HashSet<>();

    static {
        meleeReasons.add(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        meleeReasons.add(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK);
    }

    private static Set<EntityDamageByEntityEvent.DamageCause> acceptedReasons = new HashSet<>();

    static {
        acceptedReasons.addAll(meleeReasons);

        acceptedReasons.add(EntityDamageEvent.DamageCause.PROJECTILE);
        acceptedReasons.add(EntityDamageEvent.DamageCause.MAGIC);
        acceptedReasons.add(EntityDamageEvent.DamageCause.THORNS);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity defender = event.getEntity();
        Entity attacker = null;
        Projectile projectile = null;
        if (event.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) && !parent.contains(defender) && parent.contains(defender, 1)) {
            event.setCancelled(true);
            return;
        } else if (!parent.contains(defender, 1)) return;
        if (event instanceof EntityDamageByEntityEvent) attacker = ((EntityDamageByEntityEvent) event).getDamager();
        if (attacker != null) {
            if (attacker instanceof Projectile) {
                if (((Projectile) attacker).getShooter() != null) {
                    projectile = (Projectile) attacker;
                    ProjectileSource source = projectile.getShooter();
                    if (source instanceof Entity) {
                        attacker = (Entity) projectile.getShooter();
                    }
                } else if (!(attacker instanceof LivingEntity)) return;
            }
            if (defender instanceof Giant && attacker instanceof Player && !parent.contains(attacker)) {
                // Heal boss
                EntityUtil.heal(parent.boss, event.getDamage());
                // Evil code of doom
                ChatUtil.sendNotice(attacker, "Come closer...");
                attacker.teleport(parent.boss.getLocation());
                ((Player) attacker).damage(100, parent.boss);
                server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) attacker));
                attacker.setVelocity(new Vector(
                        parent.random.nextDouble() * 3 - 1.5,
                        parent.random.nextDouble() * 2,
                        parent.random.nextDouble() * 3 - 1.5
                ));
            }
        }
        if (attacker != null && !parent.contains(attacker, 1) || !parent.contains(defender, 1)) return;

        if (defender instanceof Giant) {
            final Giant boss = (Giant) defender;

            GiantBossConfig config = parent.getConfig();
            int babyCount = parent.getContained(Zombie.class).size();

            double percentageBlocked = (double) babyCount / config.bossProtectBabyCount;
            if (parent.damageHeals) {
                percentageBlocked = 3;
            }

            if (percentageBlocked > .75) {
                EntityUtil.heal(boss, event.getDamage() * percentageBlocked);
                if (acceptedReasons.contains(event.getCause())) {
                    ChatUtil.sendNotice(parent.getAudiblePlayers(), "Yes, yes, that's the spot!");
                }
            } else {
                double percentageDamageRemaining = 1 - percentageBlocked;
                event.setDamage(event.getDamage() * percentageDamageRemaining);
            }

            if (acceptedReasons.contains(event.getCause())) {
                final double oldHP = boss.getHealth();

                int maxBabySpawns = (int) (Math.pow(event.getDamage() / 30, 3) + 1);
                int babySpawns = ChanceUtil.getRandom(maxBabySpawns);
                final int chancePerSpawnPoint = Math.max(11 / babySpawns, 1);

                server.getScheduler().runTaskLater(inst, () -> {
                    if (boss.getHealth() > oldHP) return;

                    parent.spawnBabies(chancePerSpawnPoint);
                    parent.applyBabyPots();
                }, 1);
            }

            if (attacker instanceof Player) {
                if (ItemUtil.hasForgeBook((Player) attacker)) {
                    ((Giant) defender).setHealth(0);
                    final Player finalAttacker = (Player) attacker;
                    if (!finalAttacker.getGameMode().equals(GameMode.CREATIVE)) {
                        server.getScheduler().runTaskLater(inst, () -> {
                            ItemStack stack = finalAttacker.getItemInHand();
                            if (stack.getAmount() == 1) {
                                finalAttacker.setItemInHand(null);
                            } else {
                                ItemStack newStack = stack.clone();
                                newStack.setAmount(newStack.getAmount() - 1);
                                finalAttacker.setItemInHand(newStack);
                            }
                        }, 1);
                    }
                }
            }
        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (ItemUtil.hasAncientArmour(player)) {
                if (attacker != null) {
                    if (attacker instanceof Zombie) {
                        Zombie zombie = (Zombie) attacker;
                        if (zombie.isBaby() && ChanceUtil.getChance(12)) {
                            ChatUtil.sendNotice(player, "Your armour weakens the zombies.");
                            player.getNearbyEntities(8, 8, 8).stream().filter(e -> e.isValid() && e instanceof Zombie && ((Zombie) e).isBaby()).forEach(e -> ((Zombie) e).damage(18));
                        }
                    }
                    double diff = player.getMaxHealth() - player.getHealth();
                    if (ChanceUtil.getChance((int) Math.max(3, Math.round(player.getMaxHealth() - diff)))) {
                        EffectUtil.Ancient.powerBurst(player, event.getDamage());
                    }
                }
                if (parent.damageHeals && ChanceUtil.getChance(10)) {
                    ChatUtil.sendNotice(parent.getAudiblePlayers(), ChatColor.AQUA, player.getDisplayName() + " has broken the giant's spell.");
                    parent.damageHeals = false;
                }
            }

            if (attacker instanceof Giant) {
                server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) defender));

                Vector newVelocityVec = VectorUtil.createDirectionalVector(
                        parent.boss.getLocation(), defender.getLocation()
                );

                GiantBossConfig config = parent.getConfig();
                newVelocityVec.multiply(config.playerThrowForceAmplifier);
                newVelocityVec.setY(ChanceUtil.getRangedRandom(
                        config.playerThrowMinYForce,
                        config.playerThrowMaxYForce
                ));

                defender.setVelocity(newVelocityVec);

                if (parent.boss.getHealth() > parent.boss.getMaxHealth() * .5) {
                    ChatUtil.sendWarning(
                            parent.getAudiblePlayers(),
                            CollectionUtil.getElement(config.playerThrowTaunts)
                    );
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNinjaBomb(NinjaSmokeBombEvent event) {
        Player player = event.getPlayer();
        if (!parent.contains(player)) {
            return;
        }

        event.getEntities().removeIf(next -> next instanceof Giant);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity e = event.getEntity();
        if (parent.contains(e)) {
            if (parent.boss != null && e instanceof Giant) {
                List<Player> players = parent.getContainedParticipants();

                // Update high scores
                ScoreType scoreType = players.size() == 1
                        ? ScoreTypes.SHNUGGLES_PRIME_SOLO_KILLS
                        : ScoreTypes.SHNUGGLES_PRIME_TEAM_KILLS;
                for (Player aPlayer : players) {
                    parent.highScores.update(aPlayer, scoreType, 1);
                }

                int requiredBoneCount = ChanceUtil.getRandom(13) + 3;

                try {
                    for (Player player : players) {
                        boolean removed = ItemUtil.removeItemOfName(
                                player,
                                CustomItemCenter.build(CustomItems.BARBARIAN_BONE),
                                requiredBoneCount,
                                false
                        );

                        if (removed) {
                            parent.barbarianBonePlayers.add(player.getUniqueId());
                        }
                    }

                    new BoundDropSpawner(e::getLocation).provide(parent.dropTable, new MassBossKillInfo(players));
                } finally {
                    parent.barbarianBonePlayers.clear();
                }

                // Reset respawn mechanics
                parent.lastDeath = System.currentTimeMillis();
                parent.boss = null;

                // Remove remaining XP and que new xp
                for (int i = 0; i < 7; i++) {
                    server.getScheduler().runTaskLater(inst, parent.spawnXP, i * 2 * 20);
                }

                parent.setDoor(parent.eastDoor, Material.AIR);
                parent.setDoor(parent.westDoor, Material.AIR);

                parent.clearChests();
            } else if (e instanceof Zombie && ((Zombie) e).isBaby()) {
                event.getDrops().clear();
                if (ChanceUtil.getChance(28)) {
                    event.getDrops().add(new ItemStack(Material.GOLD_NUGGET, ChanceUtil.getRandom(3)));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (parent.contains(player, 1)) {
            if (parent.isBossSpawned() && parent.isParticipant(player, true)) {
                parent.handlePlayerSurrender();
            }

            try {
                parent.playerState.pushState(PlayerStateKind.SHNUGGLES_PRIME, player);
                event.getDrops().clear();
                event.setDroppedExp(0);
            } catch (ConflictingPlayerStateException | IOException e) {
                e.printStackTrace();
            }

            int number = System.currentTimeMillis() - parent.lastAttack <= 13000 ? parent.lastAttackNumber : -1;
            String deathMessage;
            switch (number) {
                case 1:
                    deathMessage = " discovered how tasty the boss's wrath is";
                    break;
                case 2:
                    deathMessage = " embraced the boss's corruption";
                    break;
                case 3:
                    deathMessage = " did not die seeing";
                    break;
                case 4:
                    deathMessage = " found out the boss has two left feet";
                    break;
                case 5:
                    deathMessage = " needs not pester invincible overlords";
                    break;
                case 6:
                    deathMessage = " died to a terrible inferno";
                    break;
                case 7:
                    deathMessage = " basked in the glory of the boss";
                    break;
                case 8:
                    deathMessage = " was the victim of a devastating prayer";
                    break;
                case 9:
                    deathMessage = " has been consumed by the boss";
                    break;
                default:
                    deathMessage = " died while attempting to slay the boss";
                    break;
            }

            event.setDeathMessage(player.getName() + deathMessage);
        }
    }

    @EventHandler
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (event.getKind() != PlayerStateKind.SHNUGGLES_PRIME_SPECTATOR) {
            return;
        }

        Player player = event.getPlayer();

        // Create a spawn point looking at something interesting.
        Location spawnPoint = CollectionUtil.getElement(parent.spawnPts).clone();
        Location pointOfInterest = parent.isBossSpawned() ? parent.boss.getLocation() : parent.getBossSpawnLocation();
        spawnPoint.setDirection(VectorUtil.createDirectionalVector(spawnPoint, pointOfInterest));

        player.teleport(spawnPoint, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }
}
