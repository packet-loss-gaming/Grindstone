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
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.collection.FiniteCache;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.type.range.worldlevel.db.PlayerWorldLevelDatabase;
import gg.packetloss.grindstone.world.type.range.worldlevel.db.mysql.MySQLPlayerWorldLevelDatabase;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ComponentInformation(friendlyName = "World Level", desc = "Operate the world level for range worlds.")
@Depend(components = {DataBaseComponent.class, ManagedWorldComponent.class, PacketInterceptionComponent.class})
public class WorldLevelComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PacketInterceptionComponent packetInterceptor;

    private PlayerWorldLevelDatabase database = new MySQLPlayerWorldLevelDatabase();
    private Map<UUID, Integer> playerWorldLevel = new HashMap<>();
    private FiniteCache<BlockVector2> recentChunks = new FiniteCache<>((int) (Bukkit.getServer().getMaxPlayers() * 1.5));

    private PlayerPlacedOresState state = new PlayerPlacedOresState();
    private SingleFileFilesystemStateHelper<PlayerPlacedOresState> stateHelper;

    protected int sourceDamageLevel = 0;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);

        CommandBook.registerEvents(new LevelAdjustmentListener(this));
        CommandBook.registerEvents(new MobListener(this));

        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("ranged-player-placed-ores.json", new TypeToken<>() { });
            stateHelper.load().ifPresent(loadedState -> state = loadedState);

            OreListener oreListener = new OreListener(this, state);
            CommandBook.registerEvents(oreListener);
        } catch (IOException e) {
            e.printStackTrace();
        }

        packetInterceptor.addListener(new HeartPacketFilter(this));

        CommandBook.server().getScheduler().runTaskTimer(
            CommandBook.inst(),
            this::degradeWorldLevel,
            0,
            20 * 5
        );
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

        int yChunk = location.getBlockY() >> 4;
        return ChanceUtil.getChance(yChunk * 25);
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
}
