/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.GiantBoss;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.city.engine.area.AreaListener;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.events.apocalypse.GemOfLifeUsageEvent;
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
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.item.BookUtil;
import com.skelril.aurora.util.item.CustomItems;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import com.skelril.aurora.util.timer.TimerUtil;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        if (parent.contains(event.getTo(), 1) && causes.contains(event.getCause())) {
            Player player = event.getPlayer();
            for (PotionEffectType potionEffectType : PotionEffectType.values()) {
                if (potionEffectType == null) continue;
                if (player.hasPotionEffect(potionEffectType)) player.removePotionEffect(potionEffectType);
            }
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

    private static Set<Class> generalBlacklistedSpecs = new HashSet<>();
    private static Set<Class> bossBlacklistedSpecs = new HashSet<>();
    private static Set<Class> ultimateBlacklistedSpecs = new HashSet<>();

    static {
        generalBlacklistedSpecs.add(GlowingFog.class);
        generalBlacklistedSpecs.add(Nightmare.class);
        generalBlacklistedSpecs.add(Disarm.class);
        generalBlacklistedSpecs.add(MobAttack.class);
        generalBlacklistedSpecs.add(FearBomb.class);

        bossBlacklistedSpecs.add(Famine.class);
        bossBlacklistedSpecs.add(LifeLeech.class);
        bossBlacklistedSpecs.add(SoulSmite.class);

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
        if (target != null && target instanceof Giant) {
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

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {
        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        LivingEntity target = event.getTarget();
        if (parent.contains(entity) && !parent.contains(target)) {
            event.setCancelled(true);
        }
    }

    private static Set<EntityDamageByEntityEvent.DamageCause> acceptedReasons = new HashSet<>();

    static {
        acceptedReasons.add(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
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
                    if (source != null && source instanceof Entity) {
                        attacker = (Entity) projectile.getShooter();
                    }
                } else if (!(attacker instanceof LivingEntity)) return;
            }
            if (defender instanceof Giant && attacker instanceof Player && !parent.contains(attacker)) {
                // Heal boss
                EntityUtil.heal(parent.boss, event.getDamage());
                // Evil code of doom
                ChatUtil.sendNotice((Player) attacker, "Come closer...");
                attacker.teleport(parent.boss.getLocation());
                ((Player) attacker).damage(parent.difficulty * 32, parent.boss);
                server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) attacker));
                attacker.setVelocity(new Vector(
                        parent.random.nextDouble() * 3 - 1.5,
                        parent.random.nextDouble() * 2,
                        parent.random.nextDouble() * 3 - 1.5
                ));
            }
            if (attacker instanceof Player) {
                Player player = (Player) attacker;
                if (defender instanceof LivingEntity) {
                    if (ItemUtil.isHoldingItem(player, CustomItems.MASTER_SWORD)) {
                        if (ChanceUtil.getChance(parent.difficulty * 3 + 1)) {
                            EffectUtil.Master.healingLight(player, (LivingEntity) defender);
                        }
                        if (ChanceUtil.getChance(parent.difficulty * 3)) {
                            List<LivingEntity> entities = player.getNearbyEntities(6, 4, 6).stream().filter(EnvironmentUtil::isHostileEntity).map(e -> (LivingEntity) e).collect(Collectors.toList());
                            EffectUtil.Master.doomBlade(player, entities);
                        }
                    }
                }
            }
        }
        if (attacker != null && !parent.contains(attacker, 1) || !parent.contains(defender, 1)) return;
        final Player[] contained = parent.getContained(Player.class);
        if (defender instanceof Giant) {
            final Giant boss = (Giant) defender;
            // Schedule a task to change the display name to show HP
            server.getScheduler().runTaskLater(inst, () -> {
                if (parent.getWorld().getTime() % 7 != 0 || !boss.isValid()) return;
                parent.printBossHealth();
            }, 1);
            if (parent.damageHeals) {
                boss.setHealth(Math.min(boss.getMaxHealth(), (event.getDamage() * parent.difficulty) + boss.getHealth()));
                if (ChanceUtil.getChance(4) && acceptedReasons.contains(event.getCause())) {
                    int affected = 0;
                    for (Entity e : boss.getNearbyEntities(8, 8, 8)) {
                        if (e.isValid() && e instanceof Player && parent.contains(e)) {
                            server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) e));
                            e.setVelocity(new Vector(
                                    Math.random() * 3 - 1.5,
                                    Math.random() * 4,
                                    Math.random() * 3 - 1.5
                            ));
                            e.setFireTicks(ChanceUtil.getRandom(20 * 60));
                            affected++;
                        }
                    }
                    if (affected > 0) {
                        ChatUtil.sendNotice(parent.getContained(1, Player.class), "Feel my power!");
                    }
                }
            }
            if (ChanceUtil.getChance(3) && acceptedReasons.contains(event.getCause())) {
                final ItemStack weapon = new ItemStack(ItemID.BONE);
                ItemMeta weaponMeta = weapon.getItemMeta();
                weaponMeta.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
                weapon.setItemMeta(weaponMeta);
                final double oldHP = boss.getHealth();
                final Entity finalAttacker = attacker;
                server.getScheduler().runTaskLater(inst, () -> {
                    if (oldHP < boss.getHealth()) return;
                    for (Location spawnPt : parent.spawnPts) {
                        if (ChanceUtil.getChance(11)) {
                            for (int i = 0; i < Math.max(3, contained.length); i++) {
                                Zombie z = (Zombie) parent.getWorld().spawnEntity(spawnPt, EntityType.ZOMBIE);
                                z.setBaby(true);
                                EntityEquipment equipment = z.getEquipment();
                                equipment.setArmorContents(null);
                                equipment.setItemInHand(weapon.clone());
                                equipment.setItemInHandDropChance(0F);
                                if (finalAttacker != null && finalAttacker instanceof LivingEntity) {
                                    z.setTarget((LivingEntity) finalAttacker);
                                }
                            }
                        }
                    }
                }, 1);
            }
            if (attacker != null && attacker instanceof Player) {
                if (ItemUtil.hasForgeBook((Player) attacker)) {
                    ((Giant) defender).setHealth(0);
                    final Player finalAttacker = (Player) attacker;
                    if (!finalAttacker.getGameMode().equals(GameMode.CREATIVE)) {
                        server.getScheduler().runTaskLater(inst, () -> (finalAttacker).setItemInHand(null), 1);
                    }
                }
            }
        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (ItemUtil.hasAncientArmour(player) && parent.difficulty >= Difficulty.HARD.getValue()) {
                if (attacker != null) {
                    if (attacker instanceof Zombie) {
                        Zombie zombie = (Zombie) attacker;
                        if (zombie.isBaby() && ChanceUtil.getChance(parent.difficulty * 5)) {
                            ChatUtil.sendNotice(player, "Your armour weakens the zombies.");
                            player.getNearbyEntities(8, 8, 8).stream().filter(e -> e.isValid() && e instanceof Zombie && ((Zombie) e).isBaby()).forEach(e -> ((Zombie) e).damage(18));
                        }
                    }
                    double diff = player.getMaxHealth() - player.getHealth();
                    if (ChanceUtil.getChance((int) Math.max(parent.difficulty, Math.round(player.getMaxHealth() - diff)))) {
                        EffectUtil.Ancient.powerBurst(player, event.getDamage());
                    }
                }
                if (ChanceUtil.getChance(parent.difficulty * 9) && defender.getFireTicks() > 0) {
                    ChatUtil.sendNotice((Player) defender, "Your armour extinguishes the fire.");
                    defender.setFireTicks(0);
                }
                if (parent.damageHeals && ChanceUtil.getChance(parent.difficulty * 3 + 1)) {
                    ChatUtil.sendNotice(parent.getContained(Player.class), ChatColor.AQUA, player.getDisplayName() + " has broken the giant's spell.");
                    parent.damageHeals = false;
                }
            }
        }
    }

    private static PotionEffect[] effects = new PotionEffect[]{
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 60 * 3, 1),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 60 * 3, 1)
    };
    private static String BARBARIAN_BONES = ChatColor.DARK_RED + "Barbarian Bone";

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity e = event.getEntity();
        if (parent.contains(e)) {
            if (parent.boss != null && e instanceof Giant) {
                Player[] players = parent.getContained(Player.class);
                Player player = null;
                int amt = players != null ? players.length : 0;
                int required = ChanceUtil.getRandom(13) + 3;
                // Figure out if someone has Barbarian Bones
                if (amt != 0) {
                    for (Player aPlayer : players) {
                        if (parent.admin.isAdmin(aPlayer)) continue;
                        if (ItemUtil.countItemsOfName(aPlayer.getInventory().getContents(), BARBARIAN_BONES) >= required) {
                            player = aPlayer;
                            break;
                        }
                    }
                }
                // Sacrificial drops
                int m = parent.getWorld().isThundering() ? 3 : 1;
                m *= player != null ? 3 : 1;
                event.getDrops().addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), m, 400000));
                event.getDrops().addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), m * 10, 4000));
                event.getDrops().addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), m * 32, 400));
                // Gold drops
                for (int i = 0; i < Math.sqrt(amt + m) + GiantBossArea.scalOffst; i++) {
                    event.getDrops().add(new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRangedRandom(32, 64)));
                }
                // Unique drops
                if (ChanceUtil.getChance(25) || m > 1 && ChanceUtil.getChance(27 / m)) {
                    event.getDrops().add(BookUtil.Lore.Monsters.skelril());
                }
                if (ChanceUtil.getChance(138) || m > 1 && ChanceUtil.getChance(84 / m)) {
                    event.getDrops().add(ItemUtil.Master.makeSword());
                }
                if (ChanceUtil.getChance(138) || m > 1 && ChanceUtil.getChance(84 / m)) {
                    event.getDrops().add(ItemUtil.Master.makeBow());
                }
                if (ChanceUtil.getChance(200) || m > 1 && ChanceUtil.getChance(108 / m)) {
                    event.getDrops().add(ItemUtil.Misc.magicBucket());
                }
                // Uber rare drops
                if (ChanceUtil.getChance(15000 / m)) {
                    event.getDrops().add(ItemUtil.Ancient.makeCrown());
                }
                // Add a few Barbarian Bones to the drop list
                event.getDrops().add(ItemUtil.Misc.barbarianBone(ChanceUtil.getRandom(Math.max(1, amt * 2))));
                // Remove the Barbarian Bones
                if (player != null) {
                    int c = ItemUtil.countItemsOfName(player.getInventory().getContents(), BARBARIAN_BONES) - required;
                    ItemStack[] nc = ItemUtil.removeItemOfName(player.getInventory().getContents(), BARBARIAN_BONES);
                    player.getInventory().setContents(nc);
                    int amount = Math.min(c, 64);
                    while (amount > 0) {
                        player.getInventory().addItem(ItemUtil.Misc.barbarianBone(amount));
                        c -= amount;
                        amount = Math.min(c, 64);
                    }
                    //noinspection deprecation
                    player.updateInventory();
                }
                LocalDate date = LocalDate.now().with(Month.APRIL).withDayOfMonth(6);
                if (date.equals(LocalDate.now())) {
                    ChatUtil.sendNotice(parent.getContained(1, Player.class), ChatColor.GOLD, "DROPS DOUBLED!");
                    event.getDrops().addAll(event.getDrops().stream().map(ItemStack::clone).collect(Collectors.toList()));
                }
                // Reset respawn mechanics
                parent.lastDeath = System.currentTimeMillis();
                parent.boss = null;
                Entity[] containedEntities = parent.getContained(Zombie.class, ExperienceOrb.class);
                // Remove remaining XP and que new xp
                parent.removeXP(containedEntities, true);
                for (int i = 0; i < 7; i++) {
                    server.getScheduler().runTaskLater(inst, parent.spawnXP, i * 2 * 20);
                }
                // Buff babies
                for (Entity entity : containedEntities) {
                    if (entity instanceof Zombie) ((Zombie) entity).addPotionEffects(Lists.newArrayList(effects));
                }
                IntegratedRunnable normal = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {
                        if (TimerUtil.matchesFilter(times, 10, 5)) {
                            ChatUtil.sendWarning(parent.getContained(1, Player.class), "Clearing chest contents in: " + times + " seconds.");
                        }
                        return true;
                    }
                    @Override
                    public void end() {
                        ChatUtil.sendWarning(parent.getContained(1, Player.class), "Clearing chest contents!");
                        for (Location location : parent.chestPts) {
                            BlockState state = location.getBlock().getState();
                            if (state instanceof Chest) {
                                ((Chest) state).getInventory().clear();
                            }
                        }
                    }
                };
                TimedRunnable timed = new TimedRunnable(normal, 30);
                BukkitTask task = server.getScheduler().runTaskTimer(inst, timed, 0, 20);
                timed.setTask(task);
            } else if (e instanceof Zombie && ((Zombie) e).isBaby()) {
                event.getDrops().clear();
                if (ChanceUtil.getChance(28)) {
                    event.getDrops().add(new ItemStack(ItemID.GOLD_NUGGET, ChanceUtil.getRandom(3)));
                }
                event.setDroppedExp(14);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        HashMap<String, PlayerState> playerState = parent.playerState;
        Player player = event.getEntity();
        if (parent.contains(player, 1) && !parent.admin.isAdmin(player) && !playerState.containsKey(player.getName())) {
            if (parent.contains(player) && parent.isBossSpawned()) {
                EntityUtil.heal(parent.boss, parent.boss.getMaxHealth() / 3);
            }
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();
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
