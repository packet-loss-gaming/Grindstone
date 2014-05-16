/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.PatientX;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.city.engine.area.AreaListener;
import com.skelril.aurora.city.engine.area.areas.DropParty.DropPartyTask;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.apocalypse.GemOfLifeUsageEvent;
import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.events.environment.CreepSpeakEvent;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import com.skelril.aurora.items.specialattack.attacks.melee.fear.Decimate;
import com.skelril.aurora.items.specialattack.attacks.melee.fear.SoulSmite;
import com.skelril.aurora.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.DoomBlade;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.Disarm;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.FearBomb;
import com.skelril.aurora.items.specialattack.attacks.ranged.misc.MobAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.unleashed.Famine;
import com.skelril.aurora.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EntityUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import com.skelril.aurora.util.item.custom.CustomItems;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
            if (!ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.PHANTOM_HYMN), 1, false)) {
                ChatUtil.sendError(player, "You need a Phantom Hymn to sacrifice to enter that area.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (parent.boss == null || !parent.contains(player) || item.getTypeId() != ItemID.SUGAR) return;
        if (ChanceUtil.getChance(5)) {
            EntityUtil.forceDamage(player, 1);
            EntityUtil.heal(parent.boss, 1);
            parent.modifyDifficulty(-.1);
        }
        event.setCancelled(true);
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
                if (source != null && source instanceof Entity) {
                    attacker = (Entity) projectile.getShooter();
                }
            } else if (!(attacker instanceof LivingEntity)) return;
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

            if (attacker instanceof Player) {
                ItemStack held = ((Player) attacker).getItemInHand();
                if (held != null && held.getTypeId() == ItemID.BLAZE_ROD) {
                    parent.modifyDifficulty(2);
                }
            }

            if (projectile != null) {
                com.sk89q.commandbook.util.entity.EntityUtil
                        .sendProjectilesFromEntity(parent.boss, 12, .5F, Snowball.class);
            }
            parent.modifyDifficulty(.5);
            parent.teleportRandom(true);
        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (attacker.equals(parent.boss)) {
                if (!ItemUtil.hasNecrosArmour(player)) {
                    event.setDamage(parent.difficulty * parent.getConfig().baseBossHit);
                }
                return;
            }
            if (ItemUtil.hasAncientArmour(player)) {
                double diff = player.getMaxHealth() - player.getHealth();
                if (ChanceUtil.getChance((int) Math.max(Math.round(parent.difficulty), Math.round(player.getMaxHealth() - diff)))) {
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
                p.getWorld().createExplosion(pt.getX(), pt.getY(), pt.getZ(), Math.round(parent.difficulty), false, false);
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
                    event.setDroppedExp(20);
                    return;
                }

                Player[] spectator = parent.getContained(Player.class);
                Player[] contained = parent.adminKit.removeAdmin(spectator);
                ChatUtil.sendWarning(spectator, "So you think you've won? Ha!");
                ChatUtil.sendWarning(spectator, "I'll get you next time...");

                List<ItemStack> drops = new ArrayList<>();
                int playerCount = spectator.length < 1 ? 1 : contained.length;
                int dropVal = parent.getConfig().playerVal * playerCount;
                drops.addAll(SacrificeComponent.getCalculatedLoot(Bukkit.getConsoleSender(), -1, dropVal));

                switch (ChanceUtil.getRandom(4)) {
                    case 1:
                        drops.add(CustomItemCenter.build(CustomItems.NECROS_HELMET));
                        break;
                    case 2:
                        drops.add(CustomItemCenter.build(CustomItems.NECROS_CHESTPLATE));
                        break;
                    case 3:
                        drops.add(CustomItemCenter.build(CustomItems.NECROS_LEGGINGS));
                        break;
                    case 4:
                        drops.add(CustomItemCenter.build(CustomItems.NECROS_BOOTS));
                        break;
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
                DropPartyTask task = new DropPartyTask(parent.getWorld(), rg, drops, 3000);
                task.setXPChance(5);
                task.setXPSize(10);
                task.start(CommandBook.inst(), server.getScheduler(), 20 * 5);
                parent.freezeBlocks(100, false);

                // Reset respawn mechanics
                parent.lastDeath = System.currentTimeMillis();
                parent.boss = null;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        HashMap<String, PlayerState> playerState = parent.playerState;
        Zombie boss = parent.boss;

        Player player = event.getEntity();
        if (parent.contains(player) && !parent.admin.isAdmin(player) && !playerState.containsKey(player.getName())) {
            if (parent.contains(player) && parent.isBossSpawned()) {
                EntityUtil.heal(boss, boss.getMaxHealth() / 4);
                parent.resetDifficulty();
                ChatUtil.sendWarning(parent.getContained(Player.class), "Haha, bow down "
                        + player.getName() + ", show's over for you.");
            }
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();

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
                    deathMessage = " has a big sweet tooth";
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
