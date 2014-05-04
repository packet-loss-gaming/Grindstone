/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.PatientX;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.area.AreaComponent;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@ComponentInformation(friendlyName = "Patient X Arena", desc = "The mad boss of Ice")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class PatientXArea extends AreaComponent<PatientXConfig> {

    @InjectComponent
    protected AdminComponent admin;

    protected static final Random random = new Random();
    protected static final int groundLevel = 54;
    protected static final int OPTION_COUNT = 7;

    protected ProtectedRegion ice;

    protected Zombie boss = null;
    protected long lastDeath = 0;
    protected long lastTelep = 0;
    protected int difficulty = 3;
    protected String targetP = "";

    protected List<Location> destinations = new ArrayList<>();
    protected final HashMap<String, PlayerState> playerState = new HashMap<>();

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            RegionManager manager = WG.getRegionManager(world);
            String base = "glacies-mare-district-mad-man";
            region = manager.getRegion(base);
            ice = manager.getRegion(base + "-ice");
            tick = 8 * 20;
            listener = new PatientXListener(this);
            config = new PatientXConfig();
        } catch (UnknownPluginException e) {
            log.info("WorldGuard could not be found!");
        }
    }

    @Override
    public void enable() {
        // WorldGuard loads late for some reason
        server.getScheduler().runTaskLater(inst, () -> {
            super.enable();

            server.getScheduler().runTaskTimer(inst, this::runAttack, 0, 20 * 20);

            destinations.add(new Location(world, -180, 54, 109.5));
            destinations.add(new Location(world, -172, 54, 120));
            destinations.add(new Location(world, -203, 58, 135.5));
            destinations.add(new Location(world, -213, 58, 116));
            destinations.add(new Location(world, -230.5, 50, 110));
            destinations.add(new Location(world, -203.5, 47, 109.5));
            destinations.add(new Location(world, -173, 47, 109.5));
            destinations.add(getCentralLoc());
        }, 1);
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
            freezeEntities();
            freezeBlocks();
            spawnCreatures();
            printBossHealth();
        }
    }

    private void equalize() {
        for (Player player : getContained(Player.class)) {
            try {
                admin.deadmin(player);

                if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    player.damage(2000, boss);
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
        LivingEntity[] entities = getContained(LivingEntity.class);
        if (entities.length > 500) {
            ChatUtil.sendWarning(getContained(Player.class), "Ring-a-round the rosie, a pocket full of posies...");
            boss.setHealth(boss.getMaxHealth());
            for (Entity entity : entities) {
                if (entity instanceof Player) {
                    if (admin.isAdmin((Player) entity)) {
                        continue;
                    }
                    ((Player) entity).setHealth(0);
                } else if (!entity.equals(boss)) {
                    entity.remove();
                }
            }
            return;
        }

        double amt = Math.pow(getContained(Player.class).length + 1, 2);
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
        ChatUtil.sendNotice(getContained(Player.class), ChatColor.DARK_AQUA, message);
    }

    private void runAttack() {
        runAttack(0);
    }

    private void runAttack(int attackCase) {

        if (!isBossSpawned()) return;

        Player[] contained = getContained(Player.class);
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
                            ChatUtil.sendWarning(getContained(Player.class), "Just\"ice\" has been served.");
                        } else {
                            player.teleport(getRandomDest());
                        }
                    }, 20 * 17);
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
            case 6:
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 60, 2));
                }
                ChatUtil.sendWarning(contained, "What's the mater, got cold feet?");
                break;
            case 7:
                for (Player player : contained) {
                    player.chat("I love Patient X!");
                    Bat b = getWorld().spawn(player.getLocation(), Bat.class);
                    b.setPassenger(player);
                }
                ChatUtil.sendWarning(contained, "Awe, I love you too!");
                ChatUtil.sendWarning(contained, "But only cause I'm a little batty...");
                break;
        }
    }

    private void freezeEntities() {
        for (LivingEntity entity : getContained(LivingEntity.class)) {
            if (entity.equals(boss)) continue;
            if (!EnvironmentUtil.isWater(entity.getLocation().getBlock())) {
                continue;
            }
            if (entity instanceof Zombie) {
                entity.setHealth(0);
                EntityUtil.heal(boss, 1);
            } else if (!ChanceUtil.getChance(5)) {
                entity.damage(ChanceUtil.getRandom(25));
            }
        }
    }

    private void freezeBlocks() {
        int minX = ice.getMinimumPoint().getBlockX();
        int maxX = ice.getMaximumPoint().getBlockX();
        int minZ = ice.getMinimumPoint().getBlockZ();
        int maxZ = ice.getMaximumPoint().getBlockZ();
        int y = ice.getMaximumPoint().getBlockY();

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getRelative(BlockFace.UP).getTypeId() == 0
                        && EnvironmentUtil.isWater(block.getRelative(BlockFace.DOWN))) {
                    if (block.getTypeId() == BlockID.PACKED_ICE) {
                        block.setTypeId(BlockID.STATIONARY_WATER);
                    } else if (ChanceUtil.getChance(config.iceChance)) {
                        block.setTypeId(BlockID.PACKED_ICE);
                    }
                }
            }
        }
    }

    public boolean isArenaLoaded() {
        Region region = new CuboidRegion(this.region.getMinimumPoint(), this.region.getMaximumPoint());
        return BukkitUtil.toLocation(getWorld(), region.getCenter()).getChunk().isLoaded();
    }

    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;

        boolean found = false;
        boolean second = false;

        for (Zombie e : getContained(Zombie.class)) {
            if (e.isValid() && !e.isBaby()) {
                if (e.getMaxHealth() == config.baseHealth + (difficulty * 100)) {
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
                e.remove();
            }
        }

        if (second) {
            for (Zombie e : getContained(Zombie.class)) {
                if (e.isValid() && !e.equals(boss)) {
                    e.remove();
                }
            }
        }
        return boss != null && boss.isValid();
    }

    public void spawnBoss() {
        boss = (Zombie) getWorld().spawnEntity(getCentralLoc(), EntityType.ZOMBIE);
        boss.setMaxHealth(config.baseHealth + (difficulty * 100));
        boss.setHealth(config.baseHealth + (difficulty * 100));
        boss.setRemoveWhenFarAway(false);
        boss.setCustomName("Patient X");

        ChatUtil.sendWarning(getContained(Player.class), "Ice to meet you again!");
    }

    private Location getCentralLoc() {
        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();

        Region region = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(getWorld(), region.getCenter().setY(groundLevel));
    }

    protected Location getRandomDest() {
        return destinations.get(ChanceUtil.getRandom(destinations.size()) - 1);
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
        ChatUtil.sendNotice(getContained(Player.class), "Pause for a second chap, I need to answer the teleport!");
    }
}
