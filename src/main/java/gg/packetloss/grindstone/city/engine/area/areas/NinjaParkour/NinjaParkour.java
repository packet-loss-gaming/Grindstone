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
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    public NinjaParkour() {
        super(1);
    }

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
        playerStateMap.values().forEach(this::clearStateColumns);
    }

    private void createColumn(BlockVector2 position) {
        int targetHeight = FLOOR_LEVEL + ChanceUtil.getRangedRandom(-4, 2);

        Block block = world.getBlockAt(position.getBlockX(), LAVA_START, position.getBlockZ());
        do {
            block.setType(Material.OBSIDIAN);

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

    private void createColumns(Player player) {
        NinjaParkourPlayerState playerState = playerStateMap.compute(player, (ignored, existingState) -> {
            if (existingState == null) {
                existingState = new NinjaParkourPlayerState();
            }

            return existingState;
        });

        if (playerState.isOnStableColumn(player)) {
            return;
        }

        playerState.cleanupPoints(player, this::clearColumn);

        BlockVector2 origin = playerState.getLastSurvivor();

        int minRange = config.columnMinRange;
        int randomRange = config.columnMaxRange - minRange;

        List<BlockVector2> columnVectors = playerState.getColumnVectors();
        for (int i = 0; i < config.columnCount; ++i) {
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
                            // Backtrack we've double selected within ourself
                            --i;
                            nearExistingColumn = true;
                            break checkNearExisting;
                        }
                    }
                }
            }

            // If near an existing column, or otherwise an invalid point, don't add it
            if (nearExistingColumn || !isValidTarget(targetColumn)) {
                continue;
            }

            // Create and add the column
            createColumn(targetColumn);
            columnVectors.add(targetColumn);
        }
    }

    private void clearColumn(BlockVector2 position) {
        Block block = world.getBlockAt(position.getBlockX(), LAVA_START, position.getBlockZ());
        while (block.getType() == Material.OBSIDIAN) {
            if (block.getY() <= LAVA_END) {
                block.setType(Material.LAVA);
            } else {
                block.setType(Material.AIR);
            }

            block = block.getRelative(BlockFace.UP);
        }
    }

    protected void teleportToStart(Player player) {
        player.teleport(resetPoint);
    }

    private void clearStateColumns(NinjaParkourPlayerState playerState) {
        for (BlockVector2 columnPosition : playerState.getColumnVectors()) {
            clearColumn(columnPosition);
        }
    }

    private void reset(Player player, NinjaParkourPlayerState playerState) {
        teleportToStart(player);
        clearStateColumns(playerState);
        playerStateMap.remove(player);
    }

    private void reset(Player player) {
        NinjaParkourPlayerState playerState = playerStateMap.get(player);
        if (playerState == null) {
            teleportToStart(player);
        } else {
            reset(player, playerState);
        }
    }

    private void finished(Player player) {
        NinjaParkourPlayerState playerState = playerStateMap.get(player);

        // This player wasn't really playing
        if (playerState == null) {
            teleportToStart(player);
            return;
        } else if (!isParticipant(player)) {
            reset(player, playerState);
            return;
        }

        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - playerState.getStartTime());

        guild.getState(player).ifPresent(guildState -> {
            int averageScore = highScores.getAverage(ScoreTypes.FASTEST_NINJA_PARKOUR).orElse(0);

            double relativePerformance = ((double) averageScore / seconds);

            double expGranted = relativePerformance * 350;
            if (guildState.grantExp(expGranted)) {
                Text text = Text.of(
                        ChatColor.YELLOW, "Your performance earns you ",
                        Text.of(ChatColor.WHITE, new DecimalFormat("#.##").format(expGranted)),
                        " experience."
                );
                player.sendMessage(text.build());
            }
        });

        highScores.update(player, ScoreTypes.FASTEST_NINJA_PARKOUR, seconds);

        reset(player, playerState);
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

    private boolean isStandingOnStoneBrick(Player player) {
        return isStandingOnBlock(player, Material.STONE_BRICKS);
    }

    private boolean isStandingOnObsidian(Player player) {
        return isStandingOnBlock(player, Material.OBSIDIAN);
    }

    private long lastCleanup = 0;

    private void tryStateCleanup() {
        if (System.currentTimeMillis() - lastCleanup < TimeUnit.SECONDS.toMillis(5)) {
            return;
        }

        Iterator<Map.Entry<Player, NinjaParkourPlayerState>> it = playerStateMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, NinjaParkourPlayerState> entry = it.next();
            Player player = entry.getKey();
            if (!player.isValid() || !contains(player)) {
                clearStateColumns(entry.getValue());
                it.remove();
            }
        }

        lastCleanup = System.currentTimeMillis();
    }

    @Override
    public void run() {
        for (Player player : getContainedParticipants()) {
            if (EnvironmentUtil.isLava(player.getLocation().getBlock())) {
                reset(player);
                continue;
            }

            if (isStandingOnStoneBrick(player)) {
                if (isOnStartingHalf(player)) {
                    createColumns(player);
                } else {
                    finished(player);
                }
            } else if (isStandingOnObsidian(player)) {
                createColumns(player);
            }
        }

        tryStateCleanup();
    }
}
