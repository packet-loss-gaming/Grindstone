/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Bessi;

import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.checker.NonSolidRegionChecker;
import gg.packetloss.grindstone.util.dropttable.BoundDropSpawner;
import gg.packetloss.grindstone.util.dropttable.MassBossDropTable;
import gg.packetloss.grindstone.util.dropttable.MassBossKillInfo;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.math.BigInteger;
import java.util.*;

@ComponentInformation(friendlyName = "Bessi", desc = "The defender of the bovine")
@Depend(components = {HighScoresComponent.class, ManagedWorldComponent.class, PlayerStateComponent.class},
        plugins = {"WorldGuard"})
public class Bessi extends AreaComponent<BessiConfig> {
    @InjectComponent
    private HighScoresComponent highScores;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    protected PlayerStateComponent playerState;

    private static final int FLOOR_LEVEL = 76;

    protected BossBar healthBar = Bukkit.createBossBar("Bessi", BarColor.PURPLE, BarStyle.SEGMENTED_6);

    protected Ravager boss = null;
    protected long lastAttack = 0;
    private int numCowsSlaughter = 0;

    protected MassBossDropTable dropTable = new MassBossDropTable();

    @Override
    public void setUp() {
        world = managedWorld.get(ManagedWorldGetQuery.CITY);
        region = WorldGuardBridge.getManagerFor(world).getRegion("carpe-diem-cow-pasture");
        tick = 20;
        listener = new BessiListener(this);
        config = new BessiConfig();

        server.getScheduler().runTaskTimer(inst, this::updateBossBarProgress, 0, 5);

        setupDropTable();
    }

    @Override
    public void disable() {
        removeBessi();
    }

    public boolean isArenaLoaded() {
        return RegionUtil.isLoaded(getWorld(), getRegion());
    }

    public boolean isBossSpawnedFast() {
        return isArenaLoaded() && boss != null && boss.isValid();
    }

    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;
        for (Ravager e : getContained(Ravager.class)) {
            if (e.isValid() && e != boss) {
                e.remove();
            }
        }

        return isBossSpawnedFast();
    }

    private boolean hasEnoughDeathForBessi() {
        return numCowsSlaughter >= config.numDeadCowsRequired;
    }

    private void sendCowsToHidingPlace() {
        for (Cow cow : getContained(Cow.class)) {
            cow.remove();
        }

        Collection<Player> audiblePlayers = getAudiblePlayers();
        ChatUtil.sendWarning(audiblePlayers, "[Some Cow] Oh Bessi!!!");
        ChatUtil.sendWarning(audiblePlayers, "The cows have gone away to their hiding place.");
    }

    private Location getRandomSpawnPoint() {
        return LocationUtil.pickLocation(world, FLOOR_LEVEL, new NonSolidRegionChecker(region, world));
    }

    private void refillCows() {
        Collection<Cow> cows = getContained(Cow.class);
        if (cows.size() < config.numCowsDesired) {
            for (int i = 0; i < config.numCowsPerTick; ++i) {
                world.spawnEntity(getRandomSpawnPoint(), EntityType.COW);
            }
        }
    }

    private void spawnBessi() {
        Validate.isTrue(!isBossSpawned());

        Location spawnPoint = getRandomSpawnPoint();

        boss = world.spawn(spawnPoint, Ravager.class);
        boss.setMaxHealth(config.bessiHealth);
        boss.setHealth(config.bessiHealth);
        boss.setRemoveWhenFarAway(false);

        EntityUtil.setMovementSpeed(boss, config.bessiSpeed);
        EntityUtil.setFollowRange(boss, 150);

        getRandomParticipant().ifPresent(boss::setTarget);
    }

    private void removeBessi() {
        if (boss != null) {
            boss.remove();
        }
        boss = null;

        getContained(ArmorStand.class).forEach(Entity::remove);
        playersThatAngerBessi.clear();
    }

    protected void markCowKilled() {
        ++numCowsSlaughter;
    }

    protected boolean isBessi(Entity entity) {
        return boss == entity;
    }

    private void setupDropTable() {
        // Guaranteed Drops
        for (int i = 0; i < 2; ++i) {
            dropTable.registerPlayerDrop(
                () -> new ItemStack(Material.COOKED_BEEF, ChanceUtil.getRandom(64))
            );
        }

        // God Items
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_SWORD));
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_BOW));

        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_AXE));
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_PICKAXE));
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_HELMET));
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_CHESTPLATE));
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_LEGGINGS));
        dropTable.registerPlayerDrop(10, () -> CustomItemCenter.build(CustomItems.GOD_BOOTS));

        dropTable.registerPlayerDrop(30, () -> CustomItemCenter.build(CustomItems.LEGENDARY_GOD_AXE));
        dropTable.registerPlayerDrop(30, () -> CustomItemCenter.build(CustomItems.LEGENDARY_GOD_PICKAXE));

        // Potions
        dropTable.registerPlayerDrop(15, () -> CustomItemCenter.build(CustomItems.EXTREME_COMBAT_POTION));
        dropTable.registerPlayerDrop(25, () -> CustomItemCenter.build(CustomItems.HOLY_COMBAT_POTION));
        dropTable.registerPlayerDrop(50, () -> CustomItemCenter.build(CustomItems.DIVINE_COMBAT_POTION));
    }

    protected Set<UUID> playersThatAngerBessi = new HashSet<>();

    public boolean isInPeanutGallery(Player player) {
        int xPos = player.getLocation().getBlockY();
        return xPos - 4 > FLOOR_LEVEL;
    }

    @Override
    public boolean isParticipant(Player player) {
        if (isInPeanutGallery(player) && !playersThatAngerBessi.contains(player.getUniqueId())) {
            return false;
        }

        return super.isParticipant(player);
    }

    private void cleanupBossAllies() {
        for (Illager illager : getContained(Illager.class)) {
            illager.remove();
        }
    }

    protected void bessiWasKilled(Location locationOfDeath) {
        cleanupBossAllies();

        Collection<Player> audiblePlayers = getAudiblePlayers();
        ChatUtil.sendNotice(audiblePlayers, "[A Cow] You've killed our savior!");
        ChatUtil.sendNotice(audiblePlayers, "[A Different Cow] Well... We might as well go back and die.");

        // Gather the players in the arena
        Collection<Player> players = getContainedParticipants();

        ScoreType scoreType = players.size() == 1
            ? ScoreTypes.BESSI_SOLO_KILLS
            : ScoreTypes.BESSI_TEAM_KILLS;

        // Update high scores
        for (Player player : players) {
            highScores.update(player, scoreType, BigInteger.ONE);
        }

        // Drop the loot
        new BoundDropSpawner(() -> locationOfDeath).provide(dropTable, new MassBossKillInfo(players));

        // Reset the number of cows slaughter and boss values
        boss = null;
        numCowsSlaughter = 0;
    }

    private void updateBossBar() {
        if (isBossSpawnedFast()) {
            BossBarUtil.syncWithPlayers(healthBar, getAudiblePlayers());
        } else {
            healthBar.removeAll();
        }
    }

    private void updateBossBarProgress() {
        if (isBossSpawnedFast()) {
            healthBar.setProgress(boss.getHealth() / boss.getMaxHealth());
        }
    }

    private Location getLocationForFireball(Player player) {
        Location rawLoc = player.getLocation();
        rawLoc.setY(100);
        rawLoc.setDirection(new Vector(0, 0, 0));
        return rawLoc;
    }

    private void runAttack() {
        int delay = ChanceUtil.getRangedRandom(config.bessiAttackDelayMin, config.bessiAttackDelayMax);
        if (System.currentTimeMillis() - lastAttack <= delay) {
            return;
        }

        Collection<Player> players = getContainedParticipants();
        Collection<Player> audiblePlayers = getAudiblePlayers();

        boss.setTarget(CollectionUtil.getElement(players));

        switch (ChanceUtil.getRandom(3)) {
            case 1:
                ChatUtil.sendWarning(audiblePlayers, "Meet the others that kill cows before you...");
                ChatUtil.sendWarning(audiblePlayers, "They're mine now... You'll be next");

                for (int i = ChanceUtil.getRangedRandom(config.bessiAlliesSpawnedMin, config.bessiAlliesSpawnedMax);
                     i > 0; --i) {
                    for (Player player : players) {
                        Location enemyLoc = getRandomSpawnPoint();
                        Monster monster = (Monster) enemyLoc.getWorld().spawnEntity(enemyLoc, EntityType.PILLAGER);
                        EntityUtil.setFollowRange(monster, 150);
                        monster.setTarget(player);
                    }
                }
                break;
            case 2:
                ChatUtil.sendWarning(audiblePlayers, "What's that in the sky? Oh yeah! It's your death!");

                List<Location> capturedLocations = new ArrayList<>();
                for (Player player : players) {
                    capturedLocations.add(getLocationForFireball(player));
                }

                {
                    TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

                    taskBuilder.setDelay(20 * 5);
                    taskBuilder.setInterval(config.bessiFireballIntervalTicks);

                    taskBuilder.setNumberOfRuns(config.bessiFireballsFired);

                    taskBuilder.setAction((times) -> {
                        if (boss == null) {
                            return true;
                        }

                        for (Location loc : capturedLocations) {
                            Entity fireball = loc.getWorld().spawnEntity(loc, EntityType.FIREBALL);
                            fireball.setVelocity(new Vector(0, -.8, 0));
                        }

                        for (Player player : getContainedParticipants()) {
                            capturedLocations.add(getLocationForFireball(player));
                        }
                        return true;
                    });

                    taskBuilder.setFinishAction(() -> {
                        if (boss != null) {
                            ChatUtil.sendWarning(getAudiblePlayers(), "Awww... It's over already?");
                        }
                    });

                    taskBuilder.build();
                }
                break;
            case 3:
                ChatUtil.sendWarning(audiblePlayers, "It's time for... The corruption!");

                {
                    TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();


                    taskBuilder.setDelay(20 * 5);
                    taskBuilder.setInterval(config.bessiCorruptionIntervalTicks);

                    taskBuilder.setNumberOfRuns(config.bessiCorruptionPhases);

                    List<Block> changedBlocks = new ArrayList<>();

                    taskBuilder.setAction((times) -> {
                        if (boss == null) {
                            return true;
                        }

                        Location targetPoint = boss.getLocation().add(0, -1, 0);

                        for (int i = ChanceUtil.getRandom(config.bessiCorruptionBlocks); i > 0; --i) {
                            if (ChanceUtil.getChance(2)) {
                                targetPoint.add(1, 0, 0);
                            } else {
                                targetPoint.add(-1, 0, 0);
                            }
                            if (ChanceUtil.getChance(2)) {
                                targetPoint.add(0, 0, 1);
                            } else {
                                targetPoint.add(0, 0, -1);
                            }

                            if (!contains(targetPoint)) {
                                break;
                            }

                            Block blockToChange = targetPoint.getBlock();
                            Material blockType = blockToChange.getType();
                            if (blockType != Material.GRASS_BLOCK) {
                                if (blockType == Material.MYCELIUM) {
                                    ++i;
                                    continue;
                                }
                                break;
                            }

                            blockToChange.setType(Material.MYCELIUM);
                            changedBlocks.add(blockToChange);
                        }

                        for (Player player : getContainedParticipants()) {
                            Location locBelowPlayer = boss.getLocation().add(0, -1, 0);

                            if (locBelowPlayer.getBlock().getType() == Material.MYCELIUM) {
                                player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.POISON,
                                    20 * 3,
                                    1
                                ));
                            }
                        }
                        return true;
                    });

                    taskBuilder.setFinishAction(() -> {
                        if (boss != null) {
                            ChatUtil.sendWarning(
                                getAudiblePlayers(),
                                "The corruption will get you next time..."
                            );
                        }
                        for (Block blockToChange : changedBlocks) {
                            blockToChange.setType(Material.GRASS_BLOCK);
                        }
                    });

                    taskBuilder.build();
                }
                break;
        }
        lastAttack = System.currentTimeMillis();
    }
    
    @Override
    public void run() {
        updateBossBar();

        if (isEmpty()) {
            if (isBossSpawned()) {
                removeBessi();
                cleanupBossAllies();
            }
            if (isArenaLoaded()) {
                refillCows();
            }
            numCowsSlaughter = 0;
            return;
        }
        
        if (!isBossSpawned()) {
            if (hasEnoughDeathForBessi()) {
                sendCowsToHidingPlace();
                spawnBessi();
            } else {
                refillCows();
            }
            return;
        }

        runAttack();
    }
}
