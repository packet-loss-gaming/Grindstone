/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.PatientX;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.regions.CuboidRegion;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackSelectEvent;
import gg.packetloss.grindstone.events.environment.CreepSpeakEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear.HellCano;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.Decimate;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.SoulSmite;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed.DoomBlade;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.SoulReaper;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.guild.ninja.Ignition;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.misc.MobAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Famine;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Surge;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.checker.NonSolidRegionChecker;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import gg.packetloss.grindstone.world.type.city.area.areas.DropParty.DropPartyTask;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

public class PatientXListener extends AreaListener<PatientXArea> {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public PatientXListener(PatientXArea parent) {
        super(parent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerTriggerEvent event) {

        if (parent.contains(event.getPlayer()) && event.getPrayer().isHoly()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (parent.contains(event.getPlayer()) || parent.contains(event.getTargeter())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseLocalSpawn(ApocalypseLocalSpawnEvent event) {
        if (parent.contains(event.getPlayer())) event.setCancelled(true);
    }

    private static final int REQUIRED_PHANTOM_ESSENCE = 20;

    @EventHandler(ignoreCancelled = true)
    public void onHymnUse(HymnSingEvent event) {
        if (event.getHymn() != HymnSingEvent.Hymn.PHANTOM) {
            return;
        }

        Player player = event.getPlayer();
        if (!LocationUtil.isInRegion(parent.getWorld(), parent.entry, player)) {
            return;
        }

        PlayerInventory pInv = player.getInventory();
        if (ItemUtil.countItemsOfName(pInv.getContents(), CustomItems.PHANTOM_ESSENCE.toString()) < REQUIRED_PHANTOM_ESSENCE) {
            ChatUtil.sendError(player, "You don't have enough phantom essence to enter.");
            return;
        }

        do {
            player.teleport(parent.getRandomDest(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        } while (parent.isBossSpawnedFast() && parent.boss.hasLineOfSight(player));

        boolean removed = ItemUtil.removeItemOfName(
                player,
                CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE),
                REQUIRED_PHANTOM_ESSENCE,
                false
        );
        Validate.isTrue(removed);

        ChatUtil.sendWarning(player, "It's been a long time since I had a worthy opponent...");
        ChatUtil.sendWarning(player, "Let's see if you have what it takes...");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (!parent.contains(from) && parent.contains(to) && !event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)) {
            Player player = event.getPlayer();
            if (player.getGameMode() != GameMode.SURVIVAL) return;

            ChatUtil.sendError(player, "This teleport isn't strong enough to reach Patient X.");
            event.setCancelled(true);
        }
    }

    private static Set<Class<?>> generalBlacklistedSpecs = new HashSet<>();
    private static Set<Class<?>> bossBlacklistedSpecs = new HashSet<>();
    private static Set<Class<?>> ultimateBlacklistedSpecs = new HashSet<>();

    static {
        generalBlacklistedSpecs.add(Nightmare.class);
        generalBlacklistedSpecs.add(Disarm.class);
        generalBlacklistedSpecs.add(MobAttack.class);
        generalBlacklistedSpecs.add(SoulReaper.class);

        bossBlacklistedSpecs.add(Famine.class);
        bossBlacklistedSpecs.add(SoulSmite.class);

        ultimateBlacklistedSpecs.add(GlowingFog.class);
        ultimateBlacklistedSpecs.add(HellCano.class);
        ultimateBlacklistedSpecs.add(Ignition.class);
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

        if (target != null && target.equals(parent.boss)) {
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
        }
    }

    private static Set<EntityDamageEvent.DamageCause> EXPLOSIVE_DAMAGE_CAUSES = new HashSet<>();

    static {
        EXPLOSIVE_DAMAGE_CAUSES.add(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
        EXPLOSIVE_DAMAGE_CAUSES.add(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
    }

    private static Set<EntityDamageByEntityEvent.DamageCause> BOSS_IGNORED_DAMAGE_CAUSES = new HashSet<>();

    static {
        BOSS_IGNORED_DAMAGE_CAUSES.add(EntityDamageEvent.DamageCause.FALL);
        BOSS_IGNORED_DAMAGE_CAUSES.addAll(EXPLOSIVE_DAMAGE_CAUSES);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {

        Entity defender = event.getEntity();
        Entity attacker = null;
        Projectile projectile = null;

        if (!parent.contains(defender)) return;

        if (event instanceof EntityDamageByEntityEvent) attacker = ((EntityDamageByEntityEvent) event).getDamager();

        if (attacker instanceof Projectile) {
            if (((Projectile) attacker).getShooter() != null) {
                projectile = (Projectile) attacker;
                ProjectileSource source = projectile.getShooter();
                if (source instanceof Entity) {
                    attacker = (Entity) projectile.getShooter();
                }
            } else if (!(attacker instanceof LivingEntity)) return;
        }

        if (defender instanceof Player && EXPLOSIVE_DAMAGE_CAUSES.contains(event.getCause())) {
            // Explosive damage formula: (1 × 1 + 1) × 8 × power + 1
            // Use 49, snowball power is 3
            double ratio = event.getDamage() / 49;
            event.setDamage(ratio * parent.difficulty);
        }

        if (defender.equals(parent.boss) && BOSS_IGNORED_DAMAGE_CAUSES.contains(event.getCause())) {
            event.setCancelled(true);
            return;
        }

        if (attacker == null || !parent.contains(attacker)) return;

        if (defender instanceof Zombie) {
            if (((Zombie) defender).isBaby()) {
                return;
            }

            double rageMultiplier = .5;

            if (attacker instanceof Player) {
                if (ItemUtil.hasItem((Player) attacker, CustomItems.CALMING_CRYSTAL)) {
                    rageMultiplier *= .25;
                }

                ItemStack held = ((Player) attacker).getInventory().getItemInMainHand();
                if (ItemUtil.isItem(held, CustomItems.PATIENT_X_THERAPY_NOTES)) {
                    rageMultiplier = -5;

                    ItemUtil.removeItemOfName(
                            (Player) attacker,
                            CustomItemCenter.build(CustomItems.PATIENT_X_THERAPY_NOTES),
                            1,
                            false
                    );
                } else if (held.getType() == Material.BLAZE_ROD) {
                    rageMultiplier += 2;
                }
            }

            if (projectile != null) {
                com.sk89q.commandbook.util.entity.EntityUtil
                        .sendProjectilesFromEntity(parent.boss, 12, .5F, Snowball.class);
            }

            int rageUnits = (int) (event.getDamage() / 50) + 1;

            parent.modifyDifficulty(rageMultiplier * rageUnits);
            parent.teleportRandom(true);
        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (attacker.equals(parent.boss)) {
                if (inst.hasPermission(player, "aurora.prayer.divinity") && ChanceUtil.getChance(parent.difficulty)) {
                    ChatUtil.sendNotice(player, "A divine force protects you.");
                    return;
                }

                for (DamageModifier modifier : DamageModifier.values()) {
                    if (event.isApplicable(modifier)) {
                        event.setDamage(modifier, 0);
                    }
                }
                event.setDamage(DamageModifier.BASE, parent.difficulty * parent.getConfig().baseBossHit);
                return;
            }
            if (ItemUtil.hasAncientArmour(player)) {
                double diff = player.getMaxHealth() - player.getHealth();
                if (ChanceUtil.getChance(Math.max(Math.round(parent.difficulty), Math.round(player.getMaxHealth() - diff)))) {
                    EffectUtil.Ancient.powerBurst(player, event.getDamage());
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile p = event.getEntity();
        if (!parent.contains(p)) return;
        if (p instanceof Snowball) {
            if (parent.boss != null && parent.boss.equals(p.getShooter())) {
                Location pt = p.getLocation();
                ExplosionStateFactory.createExplosion(pt, 3, false, false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {

        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        // Stop Patient X from becoming a drowned zombie
        if (event.getEntity() == parent.boss) {
            event.setCancelled(true);
            parent.teleportRandom();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        if (parent.contains(event.getEntity())) {
            Entity e = event.getEntity();
            if (e instanceof Zombie) {
                event.getDrops().clear();
                if (((Zombie) e).isBaby() || parent.boss == null) {
                    if (ChanceUtil.getChance(10)) {
                        event.getDrops().add(CustomItemCenter.build(CustomItems.PHANTOM_GOLD));
                    }
                    return;
                }

                Collection<Player> audible = parent.getAudiblePlayers();
                Collection<Player> contained = parent.getContainedParticipants();
                ChatUtil.sendWarning(audible, "So you think you've won? Ha!");
                ChatUtil.sendWarning(audible, "I'll get you next time...");

                int playerCount = audible.isEmpty() ? 1 : contained.size();
                int dropVal = parent.getConfig().playerVal * playerCount;
                List<ItemStack> drops = SacrificeComponent.getCalculatedLoot(Bukkit.getConsoleSender(), -1, dropVal);

                drops.add(ChanceUtil.supplyRandom(
                    () -> {
                        if (ChanceUtil.getChance(8)) {
                            return CustomItemCenter.build(CustomItems.NECROS_HELMET);
                        }
                        return CustomItemCenter.build(CustomItems.NECTRIC_HELMET);
                    },
                    () -> {
                        if (ChanceUtil.getChance(8)) {
                            return CustomItemCenter.build(CustomItems.NECROS_CHESTPLATE);
                        }
                        return CustomItemCenter.build(CustomItems.NECTRIC_CHESTPLATE);
                    },
                    () -> {
                        if (ChanceUtil.getChance(8)) {
                            return CustomItemCenter.build(CustomItems.NECROS_LEGGINGS);
                        }
                        return CustomItemCenter.build(CustomItems.NECTRIC_LEGGINGS);
                    },
                    () -> {
                        if (ChanceUtil.getChance(8)) {
                            return CustomItemCenter.build(CustomItems.NECROS_BOOTS);
                        }
                        return CustomItemCenter.build(CustomItems.NECTRIC_BOOTS);
                    }
                ));

                if (ChanceUtil.getChance(100)) {
                    drops.add(CustomItemCenter.build(CustomItems.CALMING_CRYSTAL));
                }

                for (int i = 0; i < 8 * playerCount; ++i) {
                    drops.add(CustomItemCenter.build(CustomItems.PATIENT_X_THERAPY_NOTES, ChanceUtil.getRandom(16)));
                }

                for (int i = 0; i < 8 * playerCount; ++i) {
                    drops.add(CustomItemCenter.build(CustomItems.ODE_TO_THE_FROZEN_KING, ChanceUtil.getRandom(16)));
                }

                LocalDate date = LocalDate.now().with(Month.APRIL).withDayOfMonth(6);
                if (date.equals(LocalDate.now())) {
                    ChatUtil.sendNotice(parent.getAudiblePlayers(), ChatColor.GOLD, "DROPS DOUBLED!");
                    event.getDrops().addAll(event.getDrops().stream().map(ItemStack::clone).collect(Collectors.toList()));
                }

                Location target = parent.getCentralLoc();
                ScoreType scoreType = contained.size() == 1
                        ? ScoreTypes.PATIENT_X_SOLO_KILLS
                        : ScoreTypes.PATIENT_X_TEAM_KILLS;

                for (Player player : contained) {
                    player.teleport(target);
                    Vector v = new Vector(
                            ChanceUtil.getChance(2) ? 1 : -1,
                            0,
                            ChanceUtil.getChance(2) ? 1 : -1
                    );
                    if (ChanceUtil.getChance(2)) {
                        v.setX(0);
                    } else {
                        v.setZ(0);
                    }
                    player.setVelocity(v);

                    parent.highScores.update(player, scoreType, 1);
                }
                CuboidRegion rg = new CuboidRegion(parent.drops.getMinimumPoint(), parent.drops.getMaximumPoint());
                DropPartyTask task = new DropPartyTask(
                        parent.getWorld(), rg, drops,
                        new NonSolidRegionChecker(rg, parent.getWorld())
                );
                task.setXPChance(5);
                task.setXPSize(10);
                task.start(CommandBook.inst(), server.getScheduler(), 20 * 5, 20 * 3);
                parent.freezeBlocks(100, false);

                // Reset respawn mechanics
                parent.lastDeath = System.currentTimeMillis();
                parent.boss = null;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Zombie boss = parent.boss;

        Player player = event.getEntity();
        if (parent.contains(player) && !parent.admin.isAdmin(player)) {
            if (parent.contains(player) && parent.isBossSpawned()) {
                EntityUtil.heal(boss, boss.getMaxHealth() / 4);
                parent.resetDifficulty();
                ChatUtil.sendWarning(parent.getAudiblePlayers(), "Haha, bow down "
                        + player.getName() + ", show's over for you.");
            }

            try {
                parent.playerState.pushState(PlayerStateKind.PATIENT_X, player);
                event.getDrops().clear();
                event.setDroppedExp(0);
            } catch (ConflictingPlayerStateException | IOException e) {
                e.printStackTrace();
            }

            String deathMessage;
            switch (System.currentTimeMillis() > parent.attackDur ? 0 : parent.lastAttack) {
                case 1:
                    deathMessage = " tripped over a chair";
                    break;
                case 2:
                    deathMessage = " got smashed";
                    break;
                case 3:
                    deathMessage = " bombed a performance evaluation";
                    break;
                case 4:
                    deathMessage = " became a fellow candle";
                    break;
                case 5:
                    deathMessage = " loves toxic fluids";
                    break;
                case 6:
                    deathMessage = " lost a foot or two";
                    break;
                case 7:
                    deathMessage = " went batty";
                    break;
                case 8:
                    deathMessage = " was irradiated";
                    break;
                case 9:
                    deathMessage = " took a snowball to the face";
                    break;
                default:
                    deathMessage = " froze";
                    break;
            }

            event.setDeathMessage(player.getName() + deathMessage);
        }
    }

    @EventHandler
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (event.getKind() != PlayerStateKind.PATIENT_X_SPECTATOR) {
            return;
        }

        Player player = event.getPlayer();
        player.teleport(parent.getRandomDest(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }
}
