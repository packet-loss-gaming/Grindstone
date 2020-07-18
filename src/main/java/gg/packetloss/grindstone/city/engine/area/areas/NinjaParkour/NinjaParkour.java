package gg.packetloss.grindstone.city.engine.area.areas.NinjaParkour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector2;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreEntry;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Ninja Parkour", desc = "Ninja parkour course")
@Depend(components = {ManagedWorldComponent.class, GuildComponent.class, HighScoresComponent.class},
        plugins = {"WorldGuard"})
public class NinjaParkour extends AreaComponent<NinjaParkourConfig> {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private GuildComponent guild;
    @InjectComponent
    private HighScoresComponent highScores;

    private static final int FLOOR_LEVEL = 44;
    private static final int LAVA_START = 34;
    private static final int LAVA_END = 37;

    protected Location resetPoint;

    private Map<Player, NinjaParkourPlayerState> playerStateMap = new HashMap<>();
    private List<BlockVector2> columnVectors = new ArrayList<>();
    private long generationTime = 0;

    @Override
    public void setUp() {
        world = managedWorld.get(ManagedWorldGetQuery.CITY);
        region = WorldGuardBridge.getManagerFor(world).getRegion("oblitus-district-ninja-guild-parkour");
        tick = 1;
        listener = new NinjaParkourListener(this);
        config = new NinjaParkourConfig();

        resetPoint = new Location(world, 96, 45, -510, 180, 0);

        CommandBook.registerEvents(new FlightBlockingListener(this::contains));
    }

    @Override
    public void disable() {
        columnVectors.forEach(this::clearColumn);
    }

    private void createColumn(BlockVector2 position) {
        int targetHeight = FLOOR_LEVEL + ChanceUtil.getRangedRandom(-3, 2);

        Block block = world.getBlockAt(position.getBlockX(), LAVA_START, position.getBlockZ());
        do {
            block.setType(Material.BASALT);

            block = block.getRelative(BlockFace.UP);
        } while (block.getY() < targetHeight);
    }

    private boolean isValidTarget(BlockVector2 target) {
        Block airBlock = world.getBlockAt(target.getBlockX(), FLOOR_LEVEL, target.getBlockZ());
        if (airBlock.getType() != Material.AIR) {
            return false;
        }

        Block lavaBlock = world.getBlockAt(target.getBlockX(), LAVA_END, target.getBlockZ());
        if (lavaBlock.getType() != Material.LAVA) {
            return false;
        }

        return true;
    }

    private void generateColumns(BlockVector2 origin) {
        int minRange = config.columnMinRange;
        int randomRange = config.columnMaxRange - minRange;

        int columnsGenerated = 0;
        final int columnsToGenerate = config.columnCount;
        int allowedFailuresRemaining = 10;

        while (columnsGenerated != columnsToGenerate && allowedFailuresRemaining != 0) {
            // Calculate column position
            int xAdjustment = minRange + ChanceUtil.getRandom(randomRange);
            if (ChanceUtil.getChance(2)) {
                xAdjustment = -xAdjustment;
            }
            int zAdjustment = -(minRange + ChanceUtil.getRandom(randomRange));

            BlockVector2 targetColumn = origin.add(xAdjustment, zAdjustment);

            // Check if column is near an existing column
            boolean nearExistingColumn = false;
            checkNearExisting: {
                for (int x = -1; x <= 1; ++x) {
                    for (int z = -1; z <= 1; ++z) {
                        if (columnVectors.contains(targetColumn.add(x, z))) {
                            nearExistingColumn = true;
                            break checkNearExisting;
                        }
                    }
                }
            }

            // If near an existing column, or otherwise an invalid point, don't add it
            if (nearExistingColumn || !isValidTarget(targetColumn)) {
                --allowedFailuresRemaining;
                continue;
            }

            // Create and add the column
            createColumn(targetColumn);
            columnVectors.add(targetColumn);

            ++columnsGenerated;
        }

        if (columnsGenerated == 0) {
            return;
        }

        int newColumnStart = columnVectors.size() - columnsGenerated;

        BlockVector2 furthestColumn = columnVectors.get(newColumnStart);
        for (int i = newColumnStart + 1; i < columnVectors.size(); ++i) {
            BlockVector2 column = columnVectors.get(i);
            if (column.getZ() < furthestColumn.getZ()) {
                furthestColumn = column;
            }
        }

        generateColumns(furthestColumn);
    }

    private void createColumns(Player player) {
        playerStateMap.putIfAbsent(player, new NinjaParkourPlayerState());

        if (!columnVectors.isEmpty()) {
            return;
        }

        BlockVector2 origin = WorldEditBridge.toBlockVec2(player);
        generateColumns(origin);

        generationTime = System.currentTimeMillis();
    }

    private void clearColumn(BlockVector2 position) {
        Block block = world.getBlockAt(position.getBlockX(), LAVA_START, position.getBlockZ());
        while (block.getType() == Material.BASALT) {
            if (block.getY() <= LAVA_END) {
                block.setType(Material.LAVA);
            } else {
                block.setType(Material.AIR);
            }

            block = block.getRelative(BlockFace.UP);
        }
    }

    private boolean degradeColumn(BlockVector2 position) {
        Block block = world.getBlockAt(position.getBlockX(), LAVA_START, position.getBlockZ());
        while (block.getType() == Material.BASALT) {
            Block next = block.getRelative(BlockFace.UP);

            if (next.getType() == Material.AIR) {
                if (block.getY() <= LAVA_END) {
                    block.setType(Material.LAVA);
                    return true;
                } else {
                    block.setType(Material.AIR);
                    return false;
                }
            }

            block = next;
        }
        return false;
    }

    private void tryDegradeColumns() {
        if (columnVectors.isEmpty()) {
            return;
        }

        if (System.currentTimeMillis() - generationTime < TimeUnit.SECONDS.toMillis(config.columnProtectedTime)) {
            return;
        }

        int chanceOfDegrade = config.degradeChance;
        int numToCheck = Math.max(1, columnVectors.size() / 4);

        if (playerStateMap.isEmpty()) {
            chanceOfDegrade = 1;
            numToCheck = columnVectors.size();
        }

        Iterator<BlockVector2> it = columnVectors.iterator();
        for (int i = 0; i < numToCheck; ++i) {
            BlockVector2 column = it.next();
            if (!ChanceUtil.getChance(chanceOfDegrade)) {
                continue;
            }

            if (degradeColumn(column)) {
                clearColumn(column);
                it.remove();
            }
        }
    }

    protected void teleportToStart(Player player) {
        player.teleport(resetPoint);
    }

    private void reset(Player player) {
        playerStateMap.remove(player);
        teleportToStart(player);
    }

    private long getBestTime() {
        return highScores.getBest(ScoreTypes.NINJA_PARKOUR_FASTEST_RUN).map(ScoreEntry::getScore).orElse(Long.MAX_VALUE);
    }

    private static final DecimalFormat FINE_TIME_FORMATTER = new DecimalFormat("0.000");

    private String formatElapsedTime(long elapsedTime) {
        return FINE_TIME_FORMATTER.format(elapsedTime / 1000D);
    }

    private void finished(Player player) {
        NinjaParkourPlayerState playerState = playerStateMap.get(player);

        // This player wasn't really playing
        if (playerState == null || !isParticipant(player)) {
            reset(player);
            return;
        }

        long elapsedTime = playerState.getElapsedTime();

        guild.getState(player).ifPresent(guildState -> {
            long bestTime = getBestTime();

            double relativePerformance = ((bestTime * 2.0) / elapsedTime);
            boolean newRecord = elapsedTime < bestTime;
            if (newRecord) {
                relativePerformance = config.newRecordXpMultiplier;
            }

            double expGranted = relativePerformance * config.baseXp;
            if (guildState.grantExp(expGranted)) {
                if (newRecord) {
                    ChatUtil.sendNotice(
                            getAudiblePlayers(),
                            ChatColor.GOLD + player.getDisplayName() + " set a new record!"
                    );
                }
                ChatUtil.sendNotice(
                        getAudiblePlayers(),
                        player.getDisplayName() + " successfully crossed in " +
                                ChatColor.WHITE + formatElapsedTime(elapsedTime) +
                                ChatColor.YELLOW + " seconds!"
                );

                Text text = Text.of(
                        ChatColor.YELLOW, "Your performance earns you ",
                        Text.of(ChatColor.WHITE, new DecimalFormat("#.##").format(expGranted)),
                        " experience."
                );
                player.sendMessage(text.build());
            }
        });

        highScores.update(player, ScoreTypes.NINJA_PARKOUR_CROSSINGS, 1);
        highScores.update(player, ScoreTypes.NINJA_PARKOUR_FASTEST_RUN, elapsedTime);

        reset(player);
    }

    private boolean isOnStartingHalf(Player player) {
        int centralZ = RegionUtil.getCenter(world, region).getBlockZ();
        int playerZ = player.getLocation().getBlockZ();

        // higher Z is towards towards the entrance
        return playerZ > centralZ;
    }

    private boolean isStandingOnBlock(Player player, Material blockType) {
        return player.getLocation().add(0, -1, 0).getBlock().getType() == blockType;
    }

    private void ensurePlayerStateSet(Player player) {
        if (!playerStateMap.containsKey(player)) {
            reset(player);
        }
    }

    private void sendCurrentTime(Player player, NinjaParkourPlayerState playerState, long currentTime) {
        player.sendActionBar(formatElapsedTime(playerState.getElapsedTime(currentTime)));
    }

    private void handlePlayerStates() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Player, NinjaParkourPlayerState>> it = playerStateMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, NinjaParkourPlayerState> entry = it.next();
            Player player = entry.getKey();
            if (!contains(player) || isStandingOnBlock(player, Material.SANDSTONE)) {
                it.remove();
                continue;
            }

            NinjaParkourPlayerState playerState = entry.getValue();
            sendCurrentTime(player, playerState, currentTime);
        }
    }

    @Override
    public void run() {
        for (Player player : getContainedParticipants()) {
            if (EnvironmentUtil.isLava(player.getLocation().getBlock())) {
                reset(player);
                continue;
            }

            if (isStandingOnBlock(player, Material.STONE_BRICKS)) {
                if (isOnStartingHalf(player)) {
                    createColumns(player);
                } else {
                    finished(player);
                }
            } else if (isStandingOnBlock(player, Material.BASALT)) {
                ensurePlayerStateSet(player);
            }
        }

        handlePlayerStates();
        tryDegradeColumns();
    }
}
