/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.PatientX;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.city.engine.area.PersistentArena;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.player.AdminToolkit;
import gg.packetloss.grindstone.util.player.PlayerState;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

@ComponentInformation(friendlyName = "Patient X Arena", desc = "The mad boss of Ice")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class PatientXArea extends AreaComponent<PatientXConfig> implements PersistentArena {

    @InjectComponent
    protected AdminComponent admin;

    protected static final Random random = new Random();
    protected static final int groundLevel = 54;
    protected static final int OPTION_COUNT = 9;

    protected AdminToolkit adminKit;

    protected ProtectedRegion ice, drops, entry;

    protected Zombie boss = null;
    protected long attackDur = 0;
    protected int lastAttack = 0;
    protected long lastDeath = 0;
    protected long lastTelep = 0;
    protected double difficulty;

    protected List<Location> destinations = new ArrayList<>();
    protected HashMap<String, PlayerState> playerState = new HashMap<>();

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            RegionManager manager = WG.getRegionManager(world);
            String base = "glacies-mare-district-mad-man";
            region = manager.getRegion(base);
            ice = manager.getRegion(base + "-ice");
            drops = manager.getRegion(base + "-drops");
            entry = manager.getRegion("carpe-diem-district-theater-patient-x");
            tick = 8 * 20;
            listener = new PatientXListener(this);
            config = new PatientXConfig();
            adminKit = new AdminToolkit(admin);

            server.getScheduler().runTaskTimer(inst, this::runAttack, 0, 20 * 20);

            destinations.add(new Location(world, -180, 54, 109.5));
            destinations.add(new Location(world, -173, 54, 120));
            destinations.add(new Location(world, -203, 58, 135.5));
            destinations.add(new Location(world, -213, 58, 116));
            destinations.add(new Location(world, -230.5, 50, 110));
            destinations.add(new Location(world, -203.5, 47, 109.5));
            destinations.add(new Location(world, -173, 47, 109.5));
            destinations.add(getCentralLoc());

            reloadData();
        } catch (UnknownPluginException e) {
            log.info("WorldGuard could not be found!");
        }
    }

    @Override
    public void enable() {
        // WorldGuard loads late for some reason
        server.getScheduler().runTaskLater(inst, super::enable, 1);
    }

    @Override
    public void disable() {
        writeData(false);
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
            freezeBlocks(ChanceUtil.getChance((int) Math.ceil(config.iceChangeChance - difficulty)));
            spawnCreatures();
            printBossHealth();
        }
        writeData(true);
    }

    private void equalize() {
        for (Player player : getContained(Player.class)) {
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
        Collection<LivingEntity> entities = adminKit.removeAdmin(getContained(LivingEntity.class));
        if (entities.size() > 500) {
            ChatUtil.sendWarning(getContained(Player.class), "Ring-a-round the rosie, a pocket full of posies...");
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

        double amt = adminKit.removeAdmin(getContained(Player.class)).size() * difficulty;
        Location l = getCentralLoc();
        for (int i = 0; i < amt; i++) {
            Zombie zombie = getWorld().spawn(l, Zombie.class);
            zombie.setCanPickupItems(false);
            zombie.setBaby(true);
        }
    }

    public void printBossHealth() {

        int current = (int) Math.ceil(boss.getHealth());
        int max = (int) Math.ceil(boss.getMaxHealth());

        String message = "Boss Health: " + current + " / " + max;
        double maxDiff = config.maxDifficulty - config.minDifficulty;
        double curDiff = difficulty - config.minDifficulty;
        message += " Enragement: " + (int) Math.round((curDiff / maxDiff) * 100) + "%";
        ChatUtil.sendNotice(getContained(Player.class), ChatColor.DARK_AQUA, message);
    }

    private void runAttack() {
        runAttack(0);
    }

    private void runAttack(int attackCase) {

        if (!isBossSpawned()) return;

        Collection<Player> spectator = getContained(Player.class);
        Collection<Player> contained = adminKit.removeAdmin(spectator);
        if (contained.isEmpty()) return;

        if (attackCase < 1 || attackCase > OPTION_COUNT) attackCase = ChanceUtil.getRandom(OPTION_COUNT);

        switch (attackCase) {
            case 1:
                ChatUtil.sendWarning(spectator, "Let's play musical chairs!");
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
                ChatUtil.sendWarning(spectator, "This special attack will be a \"smashing hit\"!");
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
                ChatUtil.sendWarning(spectator, "Your performance is really going to \"bomb\"!");
                break;
            case 4:
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 15, 1));
                }
                attackDur = System.currentTimeMillis() + 15750;
                ChatUtil.sendWarning(spectator, "Like a candle I hope you don't \"whither\" and die!");
                break;
            case 5:
                for (Player player : contained) {
                    for (int i = ChanceUtil.getRandom(6) + 2; i > 0; --i) {
                        DeathUtil.throwSlashPotion(player.getLocation());
                    }
                }
                attackDur = System.currentTimeMillis() + 2000;
                ChatUtil.sendWarning(spectator, "Splash to it!");
                break;
            case 6:
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 60, 2));
                }
                attackDur = System.currentTimeMillis() + 20000;
                ChatUtil.sendWarning(spectator, "What's the matter, got cold feet?");
                break;
            case 7:
                for (Player player : contained) {
                    player.chat("I love Patient X!");
                    Bat b = getWorld().spawn(player.getLocation(), Bat.class);
                    b.setPassenger(player);
                }
                attackDur = System.currentTimeMillis() + 20000;
                ChatUtil.sendWarning(spectator, "Awe, I love you too!");
                ChatUtil.sendWarning(spectator, "But only cause I'm a little batty...");
                break;
            case 8:
                server.getScheduler().runTaskLater(inst, () -> {
                    for (int i = config.radiationTimes; i > 0; i--) {
                        server.getScheduler().runTaskLater(inst, () -> {
                            if (boss != null) {
                                for (Player player : adminKit.removeAdmin(getContained(Player.class))) {
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
                ChatUtil.sendWarning(spectator, "Ahhh not the radiation treatment!");
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
                ChatUtil.sendWarning(spectator, "Let's have a snow ball fight!");
                break;
        }
        lastAttack = attackCase;
    }

    private void freezeEntities() {
        double total = 0;
        for (LivingEntity entity : adminKit.removeAdmin(getContained(LivingEntity.class))) {
            if (entity.equals(boss)) continue;
            if (!EnvironmentUtil.isWater(entity.getLocation().getBlock())) {
                continue;
            }
            if (entity instanceof Zombie) {
                entity.setHealth(0);
                EntityUtil.heal(boss, 1);
                total += .02;
            } else if (!ChanceUtil.getChance(5)) {
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
                if (block.getRelative(BlockFace.UP).getTypeId() == 0
                        && EnvironmentUtil.isWater(block.getRelative(BlockFace.DOWN))) {
                    if (percentage >= 100) {
                        block.setTypeId(BlockID.ICE);
                        continue;
                    }
                    if (block.getTypeId() == BlockID.PACKED_ICE || block.getTypeId() == BlockID.ICE) {
                        block.setTypeId(BlockID.STATIONARY_WATER);
                        if (!ChanceUtil.getChance(config.snowBallChance) || !throwExplosives) continue;
                        Location target = block.getRelative(BlockFace.UP).getLocation();
                        for (int i = ChanceUtil.getRandom(3); i > 0; i--) {
                            Snowball melvin = world.spawn(target, Snowball.class);
                            melvin.setVelocity(new Vector(0, ChanceUtil.getRangedRandom(.25, 1), 0));
                            melvin.setShooter(boss);
                        }
                    } else if (ChanceUtil.getChance(percentage, 100)) {
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
        return boss != null && boss.isValid();
    }

    public void spawnBoss() {

        resetDifficulty();
        freezeBlocks(false);

        boss = getWorld().spawn(getCentralLoc(), Zombie.class);

        // Handle vitals
        boss.setMaxHealth(config.bossHealth);
        boss.setHealth(config.bossHealth);
        boss.setRemoveWhenFarAway(false);

        // Handle items
        EntityEquipment equipment = boss.getEquipment();
        equipment.setArmorContents(null);
        equipment.setItemInHand(null);
        boss.setCanPickupItems(false);

        // Handle name
        boss.setCustomName("Patient X");

        ChatUtil.sendWarning(getContained(Player.class), "Ice to meet you again!");
    }

    protected Location getCentralLoc() {
        BlockVector min = region.getMinimumPoint();
        BlockVector max = region.getMaximumPoint();

        Region region = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(world, region.getCenter().setY(groundLevel));
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
        ChatUtil.sendNotice(getContained(Player.class), "Pause for a second chap, I need to answer the teleport!");
    }

    @Override
    public void writeData(boolean doAsync) {
        Runnable run = () -> {
            IOUtil.toBinaryFile(getWorkingDir(), "respawns", playerState);
        };

        if (doAsync) {
            server.getScheduler().runTaskAsynchronously(inst, run);
        } else {
            run.run();
        }
    }

    @Override
    public void reloadData() {
        File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
        if (playerStateFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
            if (playerStateFileO instanceof HashMap) {
                //noinspection unchecked
                playerState = (HashMap<String, PlayerState>) playerStateFileO;
                log.info("Loaded: " + playerState.size() + " respawn records for the Patient X.");
            } else {
                log.warning("Invalid block record file encountered: " + playerStateFile.getName() + "!");
                log.warning("Attempting to use backup file...");
                playerStateFile = new File(getWorkingDir().getPath() + "/old-" + playerStateFile.getName());
                if (playerStateFile.exists()) {
                    playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
                    if (playerStateFileO instanceof HashMap) {
                        //noinspection unchecked
                        playerState = (HashMap<String, PlayerState>) playerStateFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + playerState.size() + " respawn records for the Patient X.");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
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
