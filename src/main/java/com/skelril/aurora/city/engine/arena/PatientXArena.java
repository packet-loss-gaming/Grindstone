/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
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
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PatientXArena extends AbstractRegionedArena implements BossArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private static final Random random = new Random();
    private static final int groundLevel = 54;
    private static final int OPTION_COUNT = 5;

    private Zombie boss = null;
    private long lastDeath = 0;
    private long lastTelep = 0;
    private int difficulty = 3;
    private String targetP = "";

    private List<Location> destinations = new ArrayList<>();
    private final HashMap<String, PlayerState> playerState = new HashMap<>();


    public PatientXArena(World world, ProtectedRegion region, AdminComponent adminComponent) {
        super(world, region);

        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        server.getScheduler().runTaskTimer(inst, this::runAttack, 0, 20 * 20);

        destinations.add(new Location(world, -180, 54, 109.5));
        destinations.add(new Location(world, -172, 54, 120));
        destinations.add(new Location(world, -203, 58, 135.5));
        destinations.add(new Location(world, -213, 58, 116));
        destinations.add(new Location(world, -230.5, 50, 110));
        destinations.add(new Location(world, -203.5, 47, 109.5));
        destinations.add(new Location(world, -173, 47, 109.5));
        destinations.add(getCentralLoc());
    }


    public boolean isArenaLoaded() {

        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();

        Region region = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(getWorld(), region.getCenter()).getChunk().isLoaded();
    }

    @Override
    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;

        boolean found = false;
        boolean second = false;

        for (Entity e : getContainedEntities(Zombie.class)) {
            if (e.isValid() && e instanceof Zombie && !((Zombie) e).isBaby()) {
                if (!found) {
                    boss = (Zombie) e;
                    found = true;
                } else if (((Zombie) e).getHealth() < boss.getHealth()) {
                    boss = (Zombie) e;
                    second = true;
                } else {
                    e.remove();
                }
            }
        }

        if (second) {
            for (Entity e : getContainedEntities(Zombie.class)) {
                if (e.isValid() && e instanceof Zombie && !e.equals(boss)) {
                    e.remove();
                }
            }
        }
        return boss != null && boss.isValid();
    }

    @Override
    public void spawnBoss() {
        boss = (Zombie) getWorld().spawnEntity(getCentralLoc(), EntityType.ZOMBIE);
        boss.setMaxHealth(700 + (difficulty * 100));
        boss.setHealth(700 + (difficulty * 100));
        boss.setRemoveWhenFarAway(false);
        boss.setCustomName("Patient X");

        for (Player player : getContainedPlayers()) ChatUtil.sendWarning(player, "Ice to meet you again!");
    }

    private Location getCentralLoc() {
        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();

        Region region = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(getWorld(), region.getCenter().setY(groundLevel));
    }

    @Override
    public LivingEntity getBoss() {
        return boss;
    }

    @Override
    public void forceRestoreBlocks() {

    }

    @Override
    public void run() {
        if (!isBossSpawned()) {
            if (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 3) {
                spawnBoss();
            }
        } else if (!isEmpty()) {
            equalize();
            teleportRandom();
            runFreeze();
            spawnCreatures();
            printBossHealth();
        }
    }

    private void spawnCreatures() {
        Entity[] entities = getContainedEntities(LivingEntity.class);
        if (entities.length > 500) {
            ChatUtil.sendWarning(getContainedPlayers(), "Ring-a-round the rosie, a pocket full of posies...");
            boss.setHealth(boss.getMaxHealth());
            for (Entity entity : entities) {
                if (entity instanceof Player) {
                    if (adminComponent.isAdmin((Player) entity)) {
                        continue;
                    }
                    ((Player) entity).setHealth(0);
                } else if (!entity.equals(boss)) {
                    entity.remove();
                }
            }
            return;
        }

        double amt = Math.pow(getContainedPlayers().length + 1, 2);
        Location l = getCentralLoc();
        for (int i = 0; i < amt; i++) {
            Zombie zombie = getWorld().spawn(l, Zombie.class);
            zombie.setBaby(true);
        }
    }

    public void printBossHealth() {

        int current = (int) Math.ceil(boss.getHealth());
        int max = (int) Math.ceil(boss.getMaxHealth());

        String message = "Boss Health: " + current + " / " + max;
        ChatUtil.sendNotice(getContainedPlayers(), ChatColor.DARK_AQUA, message);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (contains(event.getPlayer()) && event.getCause().getEffect().getType().isHoly()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (contains(event.getPlayer()) || contains(event.getTargeter())) event.setCancelled(true);
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

        if (!contains(attack.getLocation())) return;

        Class specClass = attack.getClass();
        LivingEntity target = attack.getTarget();

        if (target != null && target instanceof Zombie) {
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

        if (!contains(defender)) return;

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

        if (attacker != null && !contains(attacker)) return;


        final Player[] contained = getContainedPlayers();

        if (defender instanceof Zombie) {
            if (((Zombie) defender).isBaby()) {
                return;
            }

            targetP = "";
            teleportRandom(true);
        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (ItemUtil.hasAncientArmour(player) && difficulty >= Difficulty.HARD.getValue()) {
                if (attacker != null) {
                    double diff = player.getMaxHealth() - player.getHealth();
                    if (ChanceUtil.getChance((int) Math.max(difficulty, Math.round(player.getMaxHealth() - diff)))) {
                        EffectUtil.Ancient.powerBurst(player, event.getDamage());
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {

        if (contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {

        Entity entity = event.getEntity();

        if (entity.equals(boss)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        if (contains(event.getEntity()) && boss != null) {
            Entity e = event.getEntity();
            if (e instanceof Zombie) {
                event.getDrops().clear();
                if (((Zombie) e).isBaby()) {
                    if (ChanceUtil.getChance(10)) {
                        event.getDrops().add(new ItemStack(ItemID.GOLD_BAR, 1));
                    }
                    event.setDroppedExp(20);
                    return;
                }

                LocalDate date = LocalDate.now().with(Month.APRIL).withDayOfMonth(6);
                if (date.equals(LocalDate.now())) {
                    ChatUtil.sendNotice(getContainedPlayers(), ChatColor.GOLD, "DROPS DOUBLED!");
                    event.getDrops().addAll(event.getDrops().stream().map(ItemStack::clone).collect(Collectors.toList()));
                }

                // Reset respawn mechanics
                lastDeath = System.currentTimeMillis();
                boss = null;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        if (contains(player) && !adminComponent.isAdmin(player) && !playerState.containsKey(player.getName())) {
            if (contains(player) && isBossSpawned()) {
                boss.setHealth(Math.min(boss.getMaxHealth(), boss.getHealth() + (boss.getMaxHealth() / 4)));
            }
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();

            int number = 0;
            String deathMessage;
            switch (number) {
                default:
                    deathMessage = " froze";
                    break;
            }

            event.setDeathMessage(player.getName() + deathMessage);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !adminComponent.isAdmin(player)) {

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

    private void teleportRandom() {
        teleportRandom(false);
    }

    private void teleportRandom(boolean force) {
        long diff = System.currentTimeMillis() - lastTelep;
        if (!force) {
            if (!ChanceUtil.getChance(4) || (lastTelep != 0 && diff < 8000)) return;
        }

        lastTelep = System.currentTimeMillis();

        boss.teleport(getRandomDest());
        ChatUtil.sendNotice(getContainedPlayers(), "Pause for a second chap, I need to answer the teleport!");
    }

    private Location getRandomDest() {
        return destinations.get(ChanceUtil.getRandom(destinations.size()) - 1);
    }

    private void runAttack() {
        runAttack(0);
    }

    private void runAttack(int attackCase) {
        Player[] contained = getContainedPlayers();
        if (contained == null || contained.length <= 0) return;

        if (attackCase < 1 || attackCase > OPTION_COUNT) attackCase = ChanceUtil.getRandom(OPTION_COUNT);

        switch (attackCase) {
            case 1:
                if (contained.length > 1) {
                    final Player player = contained[0];
                    player.teleport(new Location(getWorld(), -203.5, 50, 133));
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (player.isValid() && !contains(player)) return;
                        if (!targetP.isEmpty()) {
                            player.setHealth(0);
                            ChatUtil.sendWarning(getContainedPlayers(), "Just\"ice\" has been served.");
                        } else {
                            player.teleport(getRandomDest());
                        }
                    }, 20 * 10);
                    ChatUtil.sendWarning(contained, "Find me to save your friend...");
                    break;
                }
            case 2:
                for (Player player : contained) {
                    final double old = player.getHealth();
                    player.setHealth(3);
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (player.isValid() && !contains(player)) return;
                        player.setHealth(old * .75);
                    }, 20 * 2);
                }
                ChatUtil.sendWarning(contained, "This special attack will be a \"smashing hit\"!");
                break;
            case 3:
                for (Player player : contained) {
                    for (int i = 0; i < ChanceUtil.getRandom(10) + 5; i++) {
                        Entity e = getWorld().spawnEntity(player.getLocation(), EntityType.PRIMED_TNT);
                        e.setVelocity(new org.bukkit.util.Vector(
                                random.nextDouble() * 1 - .5,
                                random.nextDouble() * .8 + .2,
                                random.nextDouble() * 1 - .5
                        ));
                    }
                }
                ChatUtil.sendWarning(contained, "Your performance is really going to \"bomb\"!");
                break;
            case 4:
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 15, 1));
                }
                ChatUtil.sendWarning(contained, "Like a candle I hope you don't \"whither\" and die!");
                break;
            case 5:
                for (Player player : contained) {
                    for (int i = 0; i < ChanceUtil.getRandom(6) + 2; i++) {
                        DeathUtil.throwSlashPotion(player.getLocation());
                    }
                }
                ChatUtil.sendWarning(contained, "Splash to it!");
                break;
        }
    }

    private void runFreeze() {
        for (Entity entity : getContainedEntities(LivingEntity.class)) {
            if (entity.equals(boss)) continue;
            if (!EnvironmentUtil.isWater(entity.getLocation().getBlock())) {
                continue;
            }
            if (entity instanceof Zombie) {
                ((Zombie) entity).setHealth(0);
                EntityUtil.heal(boss, 1);
            } else if (!ChanceUtil.getChance(5)) {
                ((LivingEntity) entity).damage(ChanceUtil.getRandom(25));
            }
        }
    }

    @Override
    public void disable() {

    }

    @Override
    public String getId() {
        return getRegion().getId();
    }

    @Override
    public void equalize() {
        // Equalize Players
        for (Player player : getContainedPlayers()) {
            try {
                adminComponent.deadmin(player);

                if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    player.damage(2000, boss);
                }

                if (player.getVehicle() != null) {
                    player.getVehicle().eject();
                    ChatUtil.sendWarning(player, "Patient X throws you off!");
                }
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {
        return ArenaType.MONITORED;
    }
}
