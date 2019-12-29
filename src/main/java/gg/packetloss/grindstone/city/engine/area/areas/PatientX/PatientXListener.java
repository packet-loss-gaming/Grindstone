/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.PatientX;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.regions.CuboidRegion;
import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.city.engine.area.areas.DropParty.DropPartyTask;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.environment.CreepSpeakEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.Decimate;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.fear.SoulSmite;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed.DoomBlade;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.FearBomb;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.misc.MobAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.Famine;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
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
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (parent.contains(event.getPlayer()) && event.getCause().getEffect().getType().isHoly()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (parent.contains(event.getPlayer()) || parent.contains(event.getTargeter())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseLocalSpawn(ApocalypseLocalSpawnEvent event) {
        if (parent.contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnUse(HymnSingEvent event) {
        if (event.getHymn().equals(HymnSingEvent.Hymn.PHANTOM)) {
            Player player = event.getPlayer();
            if (LocationUtil.isInRegion(parent.getWorld(), parent.entry, player)) {
                boolean teleported;
                do {
                    teleported = player.teleport(parent.getRandomDest());
                } while (parent.boss.hasLineOfSight(player));
                if (teleported) {
                    ChatUtil.sendWarning(player, "It's been a long time since I had a worthy opponent...");
                    ChatUtil.sendWarning(player, "Let's see if you have what it takes...");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (parent.contains(to) && !parent.contains(from)) {
            Player player = event.getPlayer();
            if (parent.admin.isAdmin(player)) return;
            if (!ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.PHANTOM_HYMN), 1, false)) {
                ChatUtil.sendError(player, "You need a Phantom Hymn to sacrifice to enter that area.");
                event.setCancelled(true);
            }
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

    private static Set<Class> generalBlacklistedSpecs = new HashSet<>();
    private static Set<Class> bossBlacklistedSpecs = new HashSet<>();
    private static Set<Class> ultimateBlacklistedSpecs = new HashSet<>();

    static {
        generalBlacklistedSpecs.add(Nightmare.class);
        generalBlacklistedSpecs.add(Disarm.class);
        generalBlacklistedSpecs.add(MobAttack.class);
        generalBlacklistedSpecs.add(FearBomb.class);

        bossBlacklistedSpecs.add(Famine.class);
        bossBlacklistedSpecs.add(LifeLeech.class);
        bossBlacklistedSpecs.add(SoulSmite.class);

        ultimateBlacklistedSpecs.add(GlowingFog.class);
        ultimateBlacklistedSpecs.add(Decimate.class);
        ultimateBlacklistedSpecs.add(DoomBlade.class);
    }

    private long lastUltimateAttack = 0;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        SpecialAttack attack = event.getSpec();

        if (!parent.contains(attack.getLocation())) return;

        Class specClass = attack.getClass();
        LivingEntity target = attack.getTarget();

        if (target != null && target.equals(parent.boss)) {
            if (bossBlacklistedSpecs.contains(specClass)) {
                event.setCancelled(true);
                return;
            }
            if (ultimateBlacklistedSpecs.contains(specClass)) {
                if (lastUltimateAttack == 0) {
                    lastUltimateAttack = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastUltimateAttack >= 15000) {
                    lastUltimateAttack = System.currentTimeMillis();
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (generalBlacklistedSpecs.contains(specClass)) {
            event.setCancelled(true);
        }
    }

    private static Set<EntityDamageByEntityEvent.DamageCause> blockedDamage = new HashSet<>();

    static {
        blockedDamage.add(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
        blockedDamage.add(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
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

        if (defender instanceof Player && blockedDamage.contains(event.getCause())) {
            // Explosive damage formula: (1 × 1 + 1) × 8 × power + 1
            // Use 49, snowball power is 3
            double ratio = event.getDamage() / 49;
            for (DamageModifier modifier : DamageModifier.values()) {
                if (event.isApplicable(modifier)) {
                    event.setDamage(modifier, 0);
                }
            }
            event.setDamage(DamageModifier.BASE, ratio * parent.difficulty);
        }

        if (defender.equals(parent.boss) && blockedDamage.contains(event.getCause())) {
            event.setCancelled(true);
            return;
        }

        if (attacker == null || !parent.contains(attacker)) return;

        if (defender instanceof Zombie) {
            if (((Zombie) defender).isBaby()) {
                return;
            }

            double rageChange = .5;

            if (attacker instanceof Player) {
                if (ItemUtil.hasItem((Player) attacker, CustomItems.CALMING_CRYSTAL)) {
                    rageChange *= .25;
                }

                ItemStack held = ((Player) attacker).getInventory().getItemInMainHand();
                if (ItemUtil.isItem(held, CustomItems.PATIENT_X_THERAPY_NOTES)) {
                    rageChange = -5;

                    ItemUtil.removeItemOfName(
                            (Player) attacker,
                            CustomItemCenter.build(CustomItems.PATIENT_X_THERAPY_NOTES),
                            1,
                            false
                    );
                } else if (held.getType() == Material.BLAZE_ROD) {
                    rageChange += 2;
                }
            }

            if (projectile != null) {
                com.sk89q.commandbook.util.entity.EntityUtil
                        .sendProjectilesFromEntity(parent.boss, 12, .5F, Snowball.class);
            }

            parent.modifyDifficulty(rageChange);
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

                Collection<Player> spectator = parent.getContained(Player.class);
                Collection<Player> contained = parent.adminKit.removeAdmin(spectator);
                ChatUtil.sendWarning(spectator, "So you think you've won? Ha!");
                ChatUtil.sendWarning(spectator, "I'll get you next time...");

                int playerCount = spectator.isEmpty() ? 1 : contained.size();
                int dropVal = parent.getConfig().playerVal * playerCount;
                List<ItemStack> drops = SacrificeComponent.getCalculatedLoot(Bukkit.getConsoleSender(), -1, dropVal);

                switch (ChanceUtil.getRandom(4)) {
                    case 1:
                        if (ChanceUtil.getChance(8)) {
                            drops.add(CustomItemCenter.build(CustomItems.NECROS_HELMET));
                            break;
                        }
                        drops.add(CustomItemCenter.build(CustomItems.NECTRIC_HELMET));
                        break;
                    case 2:
                        if (ChanceUtil.getChance(8)) {
                            drops.add(CustomItemCenter.build(CustomItems.NECROS_CHESTPLATE));
                            break;
                        }
                        drops.add(CustomItemCenter.build(CustomItems.NECTRIC_CHESTPLATE));
                        break;
                    case 3:
                        if (ChanceUtil.getChance(8)) {
                            drops.add(CustomItemCenter.build(CustomItems.NECROS_LEGGINGS));
                            break;
                        }
                        drops.add(CustomItemCenter.build(CustomItems.NECTRIC_LEGGINGS));
                        break;
                    case 4:
                        if (ChanceUtil.getChance(8)) {
                            drops.add(CustomItemCenter.build(CustomItems.NECROS_BOOTS));
                            break;
                        }
                        drops.add(CustomItemCenter.build(CustomItems.NECTRIC_BOOTS));
                        break;
                }

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
                    ChatUtil.sendNotice(parent.getContained(Player.class), ChatColor.GOLD, "DROPS DOUBLED!");
                    event.getDrops().addAll(event.getDrops().stream().map(ItemStack::clone).collect(Collectors.toList()));
                }

                Location target = parent.getCentralLoc();
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
                }
                CuboidRegion rg = new CuboidRegion(parent.drops.getMinimumPoint(), parent.drops.getMaximumPoint());
                DropPartyTask task = new DropPartyTask(parent.getWorld(), rg, drops, new RegionChecker(rg) {
                    @Override
                    public Boolean evaluate(com.sk89q.worldedit.Vector v) {
                        Location l = new Location(parent.getWorld(), v.getX(), v.getY(), v.getZ());
                        return super.evaluate(v) && !l.getBlock().getType().isSolid();
                    }
                });
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
                ChatUtil.sendWarning(parent.getContained(Player.class), "Haha, bow down "
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
}
