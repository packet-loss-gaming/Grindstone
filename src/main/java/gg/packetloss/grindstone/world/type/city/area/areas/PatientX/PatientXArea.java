/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.PatientX;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.optimization.OptimizedZombieFactory;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.listener.BossBuggedRespawnListener;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "Patient X Arena", desc = "The mad boss of Ice")
@Depend(components = {
        AdminComponent.class, PlayerStateComponent.class, SpectatorComponent.class, HighScoresComponent.class},
        plugins = {"WorldGuard"})
public class PatientXArea extends AreaComponent<PatientXConfig> {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PlayerStateComponent playerState;
    @InjectComponent
    protected SpectatorComponent spectator;
    @InjectComponent
    protected HighScoresComponent highScores;

    protected static final Random random = new Random();
    protected static final int groundLevel = 54;
    protected static final int OPTION_COUNT = 9;

    protected ProtectedRegion ice, drops, entry;

    protected Zombie boss = null;
    protected long attackDur = 0;
    protected int lastAttack = 0;
    protected long lastDeath = 0;
    protected long lastTelep = 0;
    protected double difficulty;

    protected List<Location> destinations = new ArrayList<>();

    protected BossBar healthBar = Bukkit.createBossBar("Patient X", BarColor.WHITE, BarStyle.SEGMENTED_6);
    protected BossBar rageBar = Bukkit.createBossBar("Rage", BarColor.RED, BarStyle.SEGMENTED_6);

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.PATIENT_X_SPECTATOR);

        world = server.getWorlds().get(0);
        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "glacies-mare-district-mad-man";
        region = manager.getRegion(base);
        ice = manager.getRegion(base + "-ice");
        drops = manager.getRegion(base + "-drops");
        entry = manager.getRegion("carpe-diem-district-theater-patient-x");
        tick = 8 * 20;
        listener = new PatientXListener(this);
        config = new PatientXConfig();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));
        CommandBook.registerEvents(new BossBuggedRespawnListener(
                "Patient X",
                (e) -> boss != null && boss.equals(e) && isArenaLoaded(),
                (e) -> spawnBossEntity(e.getHealth())
        ));

        server.getScheduler().runTaskTimer(inst, (Runnable) this::runAttack, 0, 20 * 20);
        server.getScheduler().runTaskTimer(inst, this::updateBossBarProgress, 0, 5);

        destinations.add(new Location(world, -180, 54, 109.5));
        destinations.add(new Location(world, -173, 54, 120));
        destinations.add(new Location(world, -203, 58, 135.5));
        destinations.add(new Location(world, -213, 58, 116));
        destinations.add(new Location(world, -230.5, 50, 110));
        destinations.add(new Location(world, -203.5, 47, 109.5));
        destinations.add(new Location(world, -173, 47, 109.5));
        destinations.add(getCentralLoc());

        spectator.registerSpectatedRegion(PlayerStateKind.PATIENT_X_SPECTATOR, region);
        spectator.registerSpectatorSkull(
                PlayerStateKind.PATIENT_X_SPECTATOR,
                new Location(world, -421, 82, -109),
                () -> !isEmpty()
        );
    }

    @Override
    public void run() {
        updateBossBar();

        if (!isBossSpawned()) {
            if (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 10) {
                spawnBoss();
            }
        } else if (!isEmpty()) {
            equalize();
            teleportRandom();
            freezeEntities();
            freezeBlocks(ChanceUtil.getChance((int) Math.ceil(config.iceChangeChance - difficulty)));
            spawnCreatures();
        }
    }

    private void updateBossBar() {
        if (isBossSpawnedFast()) {
            Collection<Player> players = getAudiblePlayers();

            BossBarUtil.syncWithPlayers(healthBar, players);
            BossBarUtil.syncWithPlayers(rageBar, players);
        } else {
            healthBar.removeAll();
            rageBar.removeAll();
        }
    }

    private void updateBossBarProgress() {
        if (isBossSpawnedFast()) {
            // Update health
            healthBar.setProgress(boss.getHealth() / boss.getMaxHealth());

            // Update rage
            double maxDiff = config.maxDifficulty - config.minDifficulty;
            double curDiff = Math.max(difficulty, config.minDifficulty) - config.minDifficulty;
            rageBar.setProgress(curDiff / maxDiff);
        }
    }

    private void equalize() {
        for (Player player : getContainedParticipants()) {
            try {
                if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    ChatUtil.sendWarning(player, "Your defensive potion enrages me!");
                    modifyDifficulty(1);
                    player.damage(difficulty * config.baseBossHit, boss);
                }

                Entity vehicle = player.getVehicle();
                if (vehicle != null && !(vehicle instanceof Bat)) {
                    vehicle.eject();
                    ChatUtil.sendWarning(player, "Patient X throws you off!");
                }
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    private void spawnCreatures() {
        Collection<Monster> entities = getContained(Monster.class);
        if (entities.size() > 500) {
            ChatUtil.sendWarning(getAudiblePlayers(), "Ring-a-round the rosie, a pocket full of posies...");
            boss.setHealth(boss.getMaxHealth());
            for (Entity entity : entities) {
                if (entity instanceof Player) {
                    ((Player) entity).setHealth(0);
                } else if (!entity.equals(boss)) {
                    entity.remove();
                }
            }
            return;
        }

        double amt = getContainedParticipants().size() * difficulty;
        Location l = getCentralLoc();
        for (int i = 0; i < amt; i++) {
            Zombie zombie = OptimizedZombieFactory.create(l);
            zombie.setCanPickupItems(false);
            zombie.setBaby(true);
        }
    }

    private void runAttack() {
        runAttack(0);
    }

    private void runAttack(int attackCase) {

        if (!isBossSpawned()) return;

        Collection<Player> audible = getAudiblePlayers();
        Collection<Player> contained = getContainedParticipants();
        if (contained.isEmpty()) return;

        if (attackCase < 1 || attackCase > OPTION_COUNT) attackCase = ChanceUtil.getRandom(OPTION_COUNT);

        switch (attackCase) {
            case 1:
                ChatUtil.sendWarning(audible, "Let's play musical chairs!");
                for (Player player : contained) {
                    do {
                        player.teleport(getRandomDest());
                    } while (player.getLocation().distanceSquared(boss.getLocation()) <= 5 * 5);
                    if (boss.hasLineOfSight(player)) {
                        player.setHealth(ChanceUtil.getRandom(player.getMaxHealth()));
                        ChatUtil.sendWarning(player, "Don't worry, I have a medical degree...");
                        ChatUtil.sendWarning(player, "...or was that a certificate of insanity?");
                    }
                }
                attackDur = System.currentTimeMillis() + 2000;
                break;
            case 2:
                for (Player player : contained) {
                    final double old = player.getHealth();
                    player.setHealth(3);
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (player.isValid() && !contains(player)) return;
                        player.setHealth(old * .75);
                    }, 20 * 2);
                }
                attackDur = System.currentTimeMillis() + 3000;
                ChatUtil.sendWarning(audible, "This special attack will be a \"smashing hit\"!");
                break;
            case 3:
                double tntQuantity = Math.max(2, difficulty / 2.4);
                for (Player player : contained) {
                    for (double i = ChanceUtil.getRangedRandom(tntQuantity, Math.pow(2, Math.min(9, tntQuantity))); i > 0; i--) {
                        Entity e = getWorld().spawn(player.getLocation(), TNTPrimed.class);
                        e.setVelocity(new org.bukkit.util.Vector(
                                random.nextDouble() * 1 - .5,
                                random.nextDouble() * .8 + .2,
                                random.nextDouble() * 1 - .5
                        ));
                    }
                }
                attackDur = System.currentTimeMillis() + 5000;
                ChatUtil.sendWarning(audible, "Your performance is really going to \"bomb\"!");
                break;
            case 4:
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 15, 1));
                }
                attackDur = System.currentTimeMillis() + 15750;
                ChatUtil.sendWarning(audible, "Like a candle I hope you don't \"whither\" and die!");
                break;
            case 5:
                for (Player player : contained) {
                    for (int i = ChanceUtil.getRandom(6) + 2; i > 0; --i) {
                        DeathUtil.throwSlashPotion(player.getLocation());
                    }
                }
                attackDur = System.currentTimeMillis() + 2000;
                ChatUtil.sendWarning(audible, "Splash to it!");
                break;
            case 6:
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 60, 2));
                }
                attackDur = System.currentTimeMillis() + 20000;
                ChatUtil.sendWarning(audible, "What's the matter, got cold feet?");
                break;
            case 7:
                for (Player player : contained) {
                    player.chat("I love Patient X!");
                    Bat b = getWorld().spawn(player.getLocation(), Bat.class);
                    b.setPassenger(player);
                }
                attackDur = System.currentTimeMillis() + 20000;
                ChatUtil.sendWarning(audible, "Awe, I love you too!");
                ChatUtil.sendWarning(audible, "But only cause I'm a little batty...");
                break;
            case 8:
                server.getScheduler().runTaskLater(inst, () -> {
                    for (int i = config.radiationTimes; i > 0; i--) {
                        server.getScheduler().runTaskLater(inst, () -> {
                            if (boss != null) {
                                for (Player player : getContainedParticipants()) {
                                    for (int e = 0; e < 3; ++e) {
                                        Location t = LocationUtil.findRandomLoc(player.getLocation(), 5, true);
                                        for (int k = 0; k < 10; ++k) {
                                            world.playEffect(t, Effect.MOBSPAWNER_FLAMES, 0);
                                        }
                                    }
                                    if (player.getLocation().getBlock().getLightLevel() >= config.radiationLightLevel) {
                                        player.damage(difficulty * config.radiationMultiplier);
                                    }
                                }
                            }
                        }, i * 10);
                    }
                }, 3 * 20);
                attackDur = System.currentTimeMillis() + (config.radiationTimes * 500);
                ChatUtil.sendWarning(audible, "Ahhh not the radiation treatment!");
                break;
            case 9:
                final int burst = ChanceUtil.getRangedRandom(10, 20);
                server.getScheduler().runTaskLater(inst, () -> {
                    for (int i = burst; i > 0; i--) {
                        server.getScheduler().runTaskLater(inst, () -> {
                            if (boss != null) freezeBlocks(true);
                        }, i * 10);
                    }
                }, 7 * 20);
                attackDur = System.currentTimeMillis() + 7000 + (500 * burst);
                ChatUtil.sendWarning(audible, "Let's have a snow ball fight!");
                break;
        }
        lastAttack = attackCase;
    }

    private void freezeEntities() {
        double total = 0;
        for (LivingEntity entity : getContained(LivingEntity.class)) {
            if (entity.equals(boss)) continue;
            if (!EnvironmentUtil.isWater(entity.getLocation().getBlock())) {
                continue;
            }
            if (entity instanceof Zombie) {
                entity.setHealth(0);
                EntityUtil.heal(boss, 1);
                total += .02;
            } else if (entity instanceof Player && isParticipant((Player) entity) && !ChanceUtil.getChance(5)) {
                entity.damage(ChanceUtil.getRandom(25));
            }
        }
        modifyDifficulty(-total);
    }

    protected void freezeBlocks(boolean throwExplosives) {
        freezeBlocks(config.iceChance, throwExplosives);
    }

    protected void freezeBlocks(int percentage, boolean throwExplosives) {
        int minX = ice.getMinimumPoint().getBlockX();
        int maxX = ice.getMaximumPoint().getBlockX();
        int minZ = ice.getMinimumPoint().getBlockZ();
        int maxZ = ice.getMaximumPoint().getBlockZ();
        int y = ice.getMaximumPoint().getBlockY();

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getRelative(BlockFace.UP).getType() == Material.AIR
                        && EnvironmentUtil.isWater(block.getRelative(BlockFace.DOWN))) {
                    if (percentage >= 100) {
                        block.setType(Material.ICE);
                        continue;
                    }
                    if (block.getType() == Material.PACKED_ICE || block.getType() == Material.ICE) {
                        block.setType(Material.WATER);
                        if (!ChanceUtil.getChance(config.snowBallChance) || !throwExplosives) continue;
                        Location target = block.getRelative(BlockFace.UP).getLocation();
                        for (int i = ChanceUtil.getRandom(3); i > 0; i--) {
                            Snowball melvin = world.spawn(target, Snowball.class);
                            melvin.setVelocity(new Vector(0, ChanceUtil.getRangedRandom(.25, 1), 0));
                            melvin.setShooter(boss);
                        }
                    } else if (ChanceUtil.getChance(percentage, 100)) {
                        block.setType(Material.PACKED_ICE);
                    }
                }
            }
        }
    }

    public boolean isArenaLoaded() {
        return RegionUtil.isLoaded(getWorld(), getRegion());
    }

    public boolean isBossSpawnedFast() {
        return isArenaLoaded() && boss != null && boss.isValid();
    }

    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;

        boolean found = false;
        boolean second = false;

        for (Zombie e : getContained(Zombie.class)) {
            if (!e.isValid()) {
                continue;
            }

            // Skip unnamed entities
            if (e.getCustomName() == null) {
                continue;
            }

            // The boss cannot be a baby do not check them. However, if we've still have a baby zombie
            // at this point, we should not continue, as it needs removed.
            if (!e.isBaby()) {
                if (e.getMaxHealth() == config.bossHealth) {
                    if (!found) {
                        boss = e;
                        found = true;
                        continue;
                    } else if (e.getHealth() < boss.getHealth()) {
                        boss = e;
                        second = true;
                        continue;
                    }
                }
            }

            e.remove();
        }

        if (second) {
            for (Zombie e : getContained(Zombie.class)) {
                if (e.isValid() && !e.equals(boss)) {
                    e.remove();
                }
            }
        }
        return isBossSpawnedFast();
    }

    private void spawnBossEntity(double currentHealth) {
        boss = getWorld().spawn(getRandomDest(), Zombie.class, (e) -> e.getEquipment().clear());

        // Handle vitals
        boss.setMaxHealth(config.bossHealth);
        boss.setHealth(currentHealth);
        boss.setRemoveWhenFarAway(false);

        // Handle item pickup
        boss.setCanPickupItems(false);

        // Handle name
        boss.setCustomName("Patient X");

        try {
            AttributeBook.setAttribute(boss, AttributeBook.Attribute.MOVEMENT_SPEED, .5);
            AttributeBook.setAttribute(boss, AttributeBook.Attribute.FOLLOW_RANGE, 150);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }
    }

    public void spawnBoss() {
        resetDifficulty();
        freezeBlocks(false);

        spawnBossEntity(config.bossHealth);

        ChatUtil.sendWarning(getAudiblePlayers(), "Ice to meet you again!");
    }

    protected Location getCentralLoc() {
        return RegionUtil.getCenterAt(world, groundLevel, region);
    }

    protected Location getRandomDest() {
        return CollectionUtil.getElement(destinations);
    }

    protected void teleportRandom() {
        teleportRandom(false);
    }

    protected void teleportRandom(boolean force) {
        long diff = System.currentTimeMillis() - lastTelep;
        if (!force) {
            if (!ChanceUtil.getChance(4) || (lastTelep != 0 && diff < 8000)) return;
        }

        lastTelep = System.currentTimeMillis();

        boss.teleport(getRandomDest());

        List<Player> players = getContainedParticipants().stream().sorted((p1, p2) -> {
            double p1Distance = p1.getLocation().distanceSquared(boss.getLocation());
            double p2Distance = p2.getLocation().distanceSquared(boss.getLocation());
            return Double.compare(p1Distance, p2Distance);
        }).collect(Collectors.toList());

        if (!players.isEmpty()) {
            boss.setTarget(players.get(0));
        }

        ChatUtil.sendNotice(players, "Pause for a second chap, I need to answer the teleport!");
    }

    public void setDifficulty(double difficulty) {
        this.difficulty = Math.max(config.minDifficulty, Math.min(config.maxDifficulty, difficulty));
    }

    public void resetDifficulty() {
        setDifficulty(config.defaultDifficulty);
    }

    public void modifyDifficulty(double amt) {
        setDifficulty(this.difficulty + amt);
    }
}
