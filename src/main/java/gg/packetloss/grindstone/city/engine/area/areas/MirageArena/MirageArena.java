/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.block.BlockState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.city.engine.combat.PvPScope;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.BossBarUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "Mirage Arena", desc = "What will you see next?")
@Depend(components = {AdminComponent.class, SessionComponent.class, HighScoresComponent.class,
        PlayerStateComponent.class, BlockStateComponent.class},
        plugins = {"WorldGuard"})
public class MirageArena extends AreaComponent<MirageArenaConfig> {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected SessionComponent sessions;
    @InjectComponent
    protected HighScoresComponent highScores;
    @InjectComponent
    protected PlayerStateComponent playerState;
    @InjectComponent
    protected BlockStateComponent blockState;

    protected boolean voting = false;
    protected int ticks = 0;
    protected PvPScope scope;
    protected boolean editing = false;

    protected Set<BlockVector3> manuallyPlacedLocations = new HashSet<>();

    protected BossBar loadingProgressBar = Bukkit.createBossBar("Arena Loading", BarColor.BLUE, BarStyle.SEGMENTED_6);
    protected BossBar voteProgressBar = Bukkit.createBossBar("Arena Vote", BarColor.WHITE, BarStyle.SEGMENTED_6);

    public MirageArena() {
        super(1);
    }

    @Override
    public void setUp() {
        world = server.getWorlds().get(0);
        region = WorldGuardBridge.getManagerFor(world).getRegion("carpe-diem-district-mirage-arena");
        tick = 5 * 20;
        listener = new MirageArenaListener(this);
        config = new MirageArenaConfig();

        registerScope();
    }

    @Override
    public void enable() {
        super.enable();

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        MirageArenaSchematicConverter.register(registrar, this);
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            registrar.registerAsSubCommand("mirage", "Mirage Arena Control Commands", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, MirageArenaCommandsRegistration.builder(), new MirageArenaCommands(this));
            });
        });
    }

    @Override
    public void run() {
        if (!isEmpty()) {
            shiftMirage();
            revertBlocks();
        }
    }

    private void registerScope() {
        PvPComponent.registerScope(scope = new PvPScope() {
            @Override
            public boolean isApplicable(Player player) {
                return contains(player);
            }

            @Override
            public boolean allowed(Player attacker, Player defender) {
                return !sessions.getSession(MirageSession.class, attacker).isIgnored(defender.getUniqueId());
            }
        });
    }

    public void shiftMirage() {
        // Only process mirage shift logic if there is an active vote.
        if (!voting) {
            return;
        }

        // Increment ticks
        ++ticks;

        updateVoteProgress();

        if (ticks >= config.votingTicks) {
            MirageArenaSchematic next = getNextMirage(true);
            if (next == null) {
                return;
            }

            // Reset voting system
            voting = false;
            ticks = 0;
            voteProgressBar.removeAll();

            Collection<Player> players = getAudiblePlayers();
            try {
                ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, "Attempting to change the current mirage to " + next.getArenaName() + "...");
                changeMirage(next);
            } catch (CommandException e) {
                ChatUtil.sendError(players, "The arena was already changing, the vote has been cancelled!");
            } catch (IOException e) {
                e.printStackTrace();
                ChatUtil.sendError(players, "The arena could not be changed to " + next.getArenaName() + "!");
            }
        }
    }

    public void revertBlocks() {
        long currentTime = System.currentTimeMillis();
        blockState.popBlocksWhere(BlockStateKind.MIRAGE_ARENA, (blockRecord) -> {
            if (currentTime - blockRecord.getCreationTime() >= TimeUnit.MINUTES.toMillis(4)) {
                return !manuallyPlacedLocations.contains(
                        BlockVector3.at(blockRecord.getX(), blockRecord.getY(), blockRecord.getZ())
                );
            }

            return false;
        });
    }

    public MirageArenaSchematic getNextMirage(boolean clearOldVotes) {
        Map<String, ArenaVote> votes = new HashMap<>();
        getAudiblePlayers().forEach(p -> {
            MirageSession session = sessions.getSession(MirageSession.class, p);
            String vote = session.getVote();
            if (vote != null) {
                ArenaVote aVote = votes.get(vote);
                if (aVote == null) {
                    aVote = new ArenaVote(vote);
                }
                aVote.addVote();
                votes.put(vote, aVote);
            }
            if (clearOldVotes) {
                session.vote(null);
            }
        });
        List<ArenaVote> results = Lists.newArrayList(votes.values());
        results.sort((o1, o2) -> o2.getVotes() - o1.getVotes());
        return results.isEmpty() ? null : new MirageArenaSchematic(getFile(results.get(0).getArena()));
    }

    public void resetBlockRecordIndex() {
        blockState.dropAllBlocks(BlockStateKind.MIRAGE_ARENA);
    }

    public void freePlayers() {
        for (Player player : getContainedParticipants()) {
            LocationUtil.toGround(player);
        }
    }

    public void callEdit(EditSession editor, Clipboard board,
                         int cx, int cz, int cy, BlockVector3 diminsions) {
        int maxX = diminsions.getX();
        int maxY = diminsions.getY();
        int maxZ = diminsions.getZ();

        if (cy >= maxY) {
            loadingProgressBar.removeAll();
            editing = false;
            freePlayers();
            return;
        } else {
            loadingProgressBar.setProgress((double) cy / maxY);
            BossBarUtil.syncWithPlayers(loadingProgressBar, getAudiblePlayers());
        }

        long start = System.nanoTime();

        boolean timedOut = false;
        edit:
        {
            for (int x = cx; x < maxX; ++x) {
                for (int z = cz; z < maxZ; ++z) {
                    BlockVector3 relativePoint = BlockVector3.at(x, cy, z);

                    BlockVector3 placementTarget = relativePoint.add(region.getMinimumPoint());

                    BlockVector3 containedTarget = relativePoint.add(board.getMinimumPoint());
                    BlockState targetBlock = board.getBlock(containedTarget);

                    if (editor.getBlock(placementTarget).equals(targetBlock)) {
                        continue;
                    }

                    editor.rawSetBlock(placementTarget, targetBlock);

                    if (z % 30 == 0) {
                        editor.flushSession();

                        if (System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(100)) {
                            cx = x;
                            cz = z + 1;
                            timedOut = true;
                            break edit;
                        }
                    }
                }
                cz = 0;
            }
            cx = 0;
            cy++;
        }

        final int finalCy = cy;
        final int finalCz = cz;
        final int finalCx = cx;
        server.getScheduler().runTaskLater(inst, () -> {
            callEdit(editor, board, finalCx, finalCz, finalCy, diminsions);
        }, timedOut ? 10 : 1);
    }

    public void changeMirage(MirageArenaSchematic schematic) throws IOException, CommandException {
        if (editing) {
            throw new CommandException("Editing is already in progress!");
        }
        editing = true;

        File file = schematic.getPath().toFile();
        EditSession editor = WorldEditBridge.getSystemEditSessionFor(world);

        try (Closer closer = Closer.create()) {
            FileInputStream fis = closer.register(new FileInputStream(file));
            BufferedInputStream bis = closer.register(new BufferedInputStream(fis));

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            ClipboardReader reader = closer.register(format.getReader(bis));

            Clipboard clipboard = reader.read();
            resetBlockRecordIndex();

            callEdit(editor, clipboard, 0, 0, 0, clipboard.getDimensions());
        }
    }

    protected void handleBlockBreak(Block block) {
        if (manuallyPlacedLocations.remove(BlockVector3.at(block.getX(), block.getY(), block.getZ()))) {
            return;
        }

        try {
            blockState.pushAnonymousBlock(BlockStateKind.MIRAGE_ARENA, block.getState());
        } catch (UnstorableBlockStateException ignored) {
            // we don't really care, all maps are saved anyways
        }
    }

    private MirageSession getSession(Player player) {
        return sessions.getSession(MirageSession.class, player);
    }

    private void updateVoteProgress() {
        voteProgressBar.setProgress((double) ticks / config.votingTicks);
        BossBarUtil.syncWithPlayers(voteProgressBar, getAudiblePlayers());

        MirageArenaSchematic next = getNextMirage(false);
        voteProgressBar.setTitle("Arena Vote - Leader: " + next.getArenaName());
    }

    public void registerVote(Player player, MirageArenaSchematic arena) {
        getSession(player).vote(arena.getArenaName());

        voting = true;
        updateVoteProgress();
    }

    public void registerIgnore(Player player, Player target) {
        getSession(player).ignore(target.getUniqueId());
    }

    public void unregisterIgnore(Player player, Player target) {
        getSession(player).unignore(target.getUniqueId());
    }

    // FIXME: Provide a migration path from the old to new format.
    public Path getFile(String name) {
        String targetDir = getWorkingDir().getPath() + '/' + name + '/';

        Path legacyPath = Paths.get(
                targetDir,
                "arena." + BuiltInClipboardFormat.MCEDIT_SCHEMATIC.getPrimaryFileExtension()
        );
        if (Files.exists(legacyPath)) {
            return legacyPath;
        }

        return Paths.get(
                targetDir,
                "arena." + BuiltInClipboardFormat.SPONGE_SCHEMATIC.getPrimaryFileExtension()
        );
    }

    public List<MirageArenaSchematic> getArenas(String filter) {
        boolean noFilter = filter == null || filter.isBlank();
        try {
            return Files.list(getWorkingDir().toPath())
                    .filter(p -> Files.isDirectory(p))
                    .map(Path::getFileName).map(Path::toString)
                    .filter(name -> noFilter ||name.toUpperCase().contains(filter.toUpperCase()))
                    .map(this::getFile).map(MirageArenaSchematic::new)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveArena(String arenaName, boolean overwrite) throws CommandException {
        File file = getFile(arenaName.toLowerCase()).toFile();

        File directory = file.getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        } else {
            if (!overwrite) {
                throw new CommandException("An arena state by that name already exist!");
            } else if (file.exists()) {
                file.delete();
            }
        }

        EditSession editor = WorldEditBridge.getSystemEditSessionFor(world);

        try (Closer closer = Closer.create()) {
            FileOutputStream fis = closer.register(new FileOutputStream(file));
            BufferedOutputStream bos = closer.register(new BufferedOutputStream(fis));

            ClipboardWriter writer = closer.register(BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(bos));

            Region internalRegion = RegionUtil.convert(region).orElseThrow();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(internalRegion);

            ForwardExtentCopy copy = new ForwardExtentCopy(editor, internalRegion, clipboard, region.getMinimumPoint());
            copy.setCopyingEntities(false);
            copy.setCopyingBiomes(false);

            Operations.completeLegacy(copy);

            writer.write(clipboard);
        } catch (IOException | MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }
}
