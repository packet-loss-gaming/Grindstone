/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.destroystokyo.paper.Title;
import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector2;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.PacketInterceptionComponent;
import gg.packetloss.grindstone.data.DatabaseComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.collection.FiniteCache;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import gg.packetloss.grindstone.util.probability.WeightedPicker;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.type.range.worldlevel.db.PlayerWorldLevelDatabase;
import gg.packetloss.grindstone.world.type.range.worldlevel.db.sql.SQLPlayerWorldLevelDatabase;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.Fangz;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.FearKnight;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.StormBringer;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ComponentInformation(friendlyName = "World Level", desc = "Operate the world level for range worlds.")
@Depend(components = {DatabaseComponent.class, ManagedWorldComponent.class, PacketInterceptionComponent.class})
public class WorldLevelComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PacketInterceptionComponent packetInterceptor;

    private PlayerWorldLevelDatabase database = new SQLPlayerWorldLevelDatabase();
    private Map<UUID, Integer> playerWorldLevel = new HashMap<>();
    private FiniteCache<BlockVector2> recentChunks = new FiniteCache<>((int) (Bukkit.getServer().getMaxPlayers() * 1.5));

    private PlayerPlacedOresState state = new PlayerPlacedOresState();
    private SingleFileFilesystemStateHelper<PlayerPlacedOresState> stateHelper;

    private DemonicRuneListener demonicRunes;
    private WorldLevelConfig config;
    private BukkitTask minibossCheckTask;
    private RangeWorldMinibossTargetWatcher minibossTargetWatcher;


    protected int sourceDamageLevel = 0;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);

        CommandBook.registerEvents(demonicRunes = new DemonicRuneListener(this));
        CommandBook.registerEvents(new LevelAdjustmentListener(this));
        CommandBook.registerEvents(new MobListener(this));
        CommandBook.registerEvents(new SilverfishClusterListener(this));

        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("ranged-player-placed-ores.json", new TypeToken<>() { });
            stateHelper.load().ifPresent(loadedState -> state = loadedState);

            OreListener oreListener = new OreListener(this, state);
            CommandBook.registerEvents(oreListener);
        } catch (IOException e) {
            e.printStackTrace();
        }

        config = configure(new WorldLevelConfig());

        packetInterceptor.addListener(new HeartPacketFilter(this));

        Bukkit.getScheduler().runTaskTimer(
            CommandBook.inst(),
            this::degradeWorldLevel,
            0,
            20 * 5
        );
        CommandBook.registerEvents(minibossTargetWatcher = new RangeWorldMinibossTargetWatcher(this));
        configureMiniBosses();
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
        configureMiniBosses();
    }

    public WorldLevelConfig getConfig() {
        return config;
    }

    public void degradeWorldLevel() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isPeaceful(player)) {
                continue;
            }

            int currLevel = getWorldLevel(player);
            if (currLevel > 1) {
                int newLevel = currLevel - 1;

                setWorldLevel(player, newLevel);
                showTitleForLevel(player, newLevel);
            }
        }
    }

    protected RangeWorldMinibossTargetWatcher getMinibossTargetWatcher() {
        return minibossTargetWatcher;
    }

    private WeightedPicker<RangeWorldMinibossSpawner> minibossSpawners;

    private final Fangz fangz = new Fangz(this);
    private final StormBringer stormBringer = new StormBringer(this);
    private final FearKnight fearKnight = new FearKnight(this);

    private void configureMiniBosses() {
        if (minibossCheckTask != null) {
            minibossCheckTask.cancel();
        }

        minibossCheckTask = Bukkit.getScheduler().runTaskTimer(
            CommandBook.inst(),
            this::tryPromoteMiniBosses,
            0,
            config.miniBossCheckFrequency
        );

        minibossSpawners = new WeightedPicker<>();

        minibossSpawners.add(fangz, config.miniBossFangzSpawnWeight);
        minibossSpawners.add(fearKnight, config.miniBossFearKnightSpawnWeight);
        minibossSpawners.add(stormBringer, config.miniBossStormBringerSpawnWeight);
    }

    public void tryPromoteMiniBosses() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isRangeWorld(player.getWorld())) {
                continue;
            }

            int currLevel = getWorldLevel(player);
            if (currLevel < config.miniBossMinimumWorldLevel) {
                continue;
            }

            if (!ChanceUtil.getChance(config.minibossPromotionChance)) {
                continue;
            }

            tryCreateMinibossNear(player, currLevel);
        }
    }


    private void createMinibossAt(Location targetLoc, Player target, int worldLevel) {
        RangeWorldMinibossSpawner bossSpawner = minibossSpawners.pick();
        bossSpawner.spawnBoss(targetLoc, target, worldLevel);
    }

    private boolean isValidMinibossSpawnLocation(Player targetPLayer, Location location) {
        if (!LocationUtil.isChunkLoadedAt(location)) {
            return false;
        }

        Block locBlock = location.getBlock();
        if (locBlock.getLightLevel() >= 8) {
            return false;
        }

        if (targetPLayer.hasLineOfSight(location)) {
            return false;
        }

        return true;
    }

    private void tryCreateMinibossNear(Player player, int worldLevel) {
        Location targetLoc;
        int runs = 0;
        do {
            targetLoc = LocationUtil.findRandomLoc(player.getLocation(), 50, true);
            if (!isValidMinibossSpawnLocation(player, targetLoc)) {
                targetLoc = null;
            }
            ++runs;
        } while (targetLoc == null && runs <= config.miniBossMaximumSpawnTries);

        if (targetLoc == null) {
            return;
        }

        createMinibossAt(targetLoc, player, worldLevel);
    }

    @Override
    public void disable() {
        try {
            for (Map.Entry<UUID, Integer> entry: playerWorldLevel.entrySet()) {
                update(entry.getKey(), entry.getValue());
            }

            stateHelper.save(state);
        } catch (IOException e) {
            e.printStackTrace();
        }

        demonicRunes.finishPortalsNow();
    }

    private TaskFuture<Optional<Integer>> loadWorldLevel(Player player) {
        return TaskFuture.asyncTask(() -> {
            return database.loadWorldLevel(player.getUniqueId());
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        loadWorldLevel(player).thenAccept((optWorldLevel) -> {
            optWorldLevel.ifPresent((worldLevel -> {
                playerWorldLevel.put(player.getUniqueId(), worldLevel);
                if (isRangeWorld(player.getWorld())) {
                    showTitleForLevel(player, worldLevel);
                }
            }));
        });
    }

    private void update(UUID playerID, int worldLevel) {
        database.updateWorldLevel(playerID, worldLevel);
    }

    private void update(Player player, int worldLevel) {
        update(player.getUniqueId(), worldLevel);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Integer worldLevel = playerWorldLevel.remove(player.getUniqueId());
        if (worldLevel == null) {
            return;
        }

        PluginTaskExecutor.submitAsync(() -> {
            update(player, worldLevel);
        });
    }

    public boolean isPeaceful(Player player) {
        return ItemUtil.hasPeacefulWarriorArmor(player);
    }

    public int getWorldLevel(Player player) {
        return playerWorldLevel.getOrDefault(player.getUniqueId(), 1);
    }

    public void setWorldLevel(Player player, int worldLevel) {
        Validate.isTrue(worldLevel >= 1);
        UUID playerID = player.getUniqueId();
        playerWorldLevel.put(playerID, worldLevel);
    }

    protected void showTitleForLevel(Player player, int newLevel) {
        player.sendTitle(Title.builder().title(
            Text.of(
                ChatColor.DARK_RED,
                "WORLD LEVEL"
            ).build()
        ).subtitle(
            Text.of(
                ChatColor.DARK_RED,
                newLevel
            ).build()
        ).fadeIn(10).stay(20).fadeOut(10).build());
    }

    protected void showTitleForLevelIfInteresting(Player player) {
        int level = getWorldLevel(player);
        if (level == 1) {
            return;
        }

        showTitleForLevel(player, level);
    }

    protected boolean isRangeWorld(World world) {
        return managedWorld.is(ManagedWorldIsQuery.ANY_RANGE, world);
    }

    protected boolean shouldSpawnDemonicAshes(Player player, Location location, Material blockType) {
        if (isPeaceful(player)) {
            return false;
        }

        if (recentChunks.contains(WorldEditBridge.toBlockVec2(location.getChunk()))) {
            return false;
        }

        if (blockType.getHardness() < 2.0F) {
            return false;
        }

        if (blockType.isBurnable()) {
            return false;
        }

        // Subtract the minimum world height, if this is 0 it has no effect, if it's lower than zero, it adds a
        // positive number to all coordinates, which in turn results in a larger gradient of vertical chunks.
        int worldMinY = location.getWorld().getMinHeight();
        int yChunk = (location.getBlockY() - worldMinY) >> 4;

        return ChanceUtil.getChance(yChunk * config.demonicAshesPerYChunk);
    }

    protected int getDropCountModifier(int worldLevel, double monsterTypeModifier, double percentDamageDone) {
        return Math.max(
            1,
            (int) Math.min(
                config.mobsDropTableItemCountMax,
                monsterTypeModifier * config.mobsDropTableItemCountPerLevel * worldLevel * percentDamageDone
            )
        );
    }

    protected double getDropValueModifier(int worldLevel, double monsterTypeModifier, double percentDamageDone) {
        return monsterTypeModifier * worldLevel * percentDamageDone;
    }

    protected void spawnDemonicAshes(Player player, Location location) {
        recentChunks.add(WorldEditBridge.toBlockVec2(location.getChunk()));

        EntityUtil.spawnProtectedItem(CustomItemCenter.build(CustomItems.DEMONIC_ASHES), player);
    }

    protected double scale(int fromLevel, int toLevel) {
        return (double) toLevel / fromLevel;
    }

    protected double scaleHealth(double health, int fromLevel, int toLevel) {
        double scale = scale(fromLevel, toLevel);
        if (scale > 1) {
            return health * scale * 5;
        } else {
            return (health * scale) / 5;
        }
    }

    protected boolean hasScaledHealth(Entity entity) {
        return EntityUtil.isHostileMob(entity) && entity.getCustomName() == null && isRangeWorld(entity.getWorld());
    }

    public static double scaleDamageForLevel(double baseDamage, int level) {
        return baseDamage + ChanceUtil.getRandomNTimes(level, 2) - 1;
    }
}
