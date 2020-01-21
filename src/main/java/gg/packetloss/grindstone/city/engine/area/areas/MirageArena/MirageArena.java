/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.PaginatedResult;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
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
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @Override
    public void setUp() {
        world = server.getWorlds().get(0);
        region = WorldGuardBridge.getManagerFor(world).getRegion("carpe-diem-district-mirage-arena");
        tick = 5 * 20;
        listener = new MirageArenaListener(this);
        config = new MirageArenaConfig();

        registerScope();
        registerCommands(Commands.class);
    }

    @Override
    public void enable() {
        // WorldGuard loads late for some reason
        server.getScheduler().runTaskLater(inst, super::enable, 1);
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
                return !sessions.getSession(MirageSession.class, attacker).isIgnored(defender.getName());
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

        if (ticks >= 60) {
            String next = getNextMirage(true);
            if (next == null) {
                return;
            }

            // Reset voting system
            voting = false;
            ticks = 0;

            Collection<Player> players = getAudiblePlayers();
            try {
                ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, "Attempting to change the current mirage to " + next + "...");
                changeMirage(next);
            } catch (CommandException e) {
                ChatUtil.sendError(players, "The arena was already changing, the vote has been cancelled!");
            } catch (IOException e) {
                e.printStackTrace();
                ChatUtil.sendError(players, "The arena could not be changed to " + next + "!");
            }
        } else if (ticks % 10 == 0) {
            String next = getNextMirage(false);
            if (next == null) {
                return;
            }

            Collection<Player> players = getAudiblePlayers();
            ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, "The currently winning mirage is " + next + '.');
            ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, ((60 - ticks) * 5) + " seconds til arena change.");
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

    public String getNextMirage(boolean clearOldVotes) {
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
        return results.isEmpty() ? null : results.get(0).getArena();
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
            ChatUtil.sendNotice(getAudiblePlayers(), "Editing Completed.");
            editing = false;
            freePlayers();
            return;
        } else if (cx == 0 && cy % 10 == 0) {
            ChatUtil.sendNotice(getAudiblePlayers(), "Editing Layer: " + cy + '/' + maxY);
        }

        long start = System.nanoTime();

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
        }, 10);
    }

    public void changeMirage(String newMirage) throws IOException, CommandException {
        File file = getFile(newMirage);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }

        if (editing) {
            throw new CommandException("Editing is already in progress!");
        }
        editing = true;

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

    // FIXME: Provide a migration path from the old to new format.
    public File getFile(String name) {
        String targetDir = getWorkingDir().getPath() + '/' + name + '/';

        Path legacyPath = Paths.get(
                targetDir,
                "arena." + BuiltInClipboardFormat.MCEDIT_SCHEMATIC.getPrimaryFileExtension()
        );
        if (Files.exists(legacyPath)) {
            return legacyPath.toFile();
        }

        return Paths.get(
                targetDir,
                "arena." + BuiltInClipboardFormat.SPONGE_SCHEMATIC.getPrimaryFileExtension()
        ).toFile();
    }

    public class Commands {
        @Command(aliases = {"mirage"}, desc = "Mirage Commands")
        @NestedCommand({MirageCommands.class})
        public void profileCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class MirageCommands {

        @Command(aliases = {"vote"},
                usage = "<arena>", desc = "Vote for a mirage arena",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.vote")
        public void vote(CommandContext args, CommandSender sender) throws CommandException {
            String initFile = args.getString(0);
            File file = getFile(initFile);

            if (!file.exists()) {
                throw new CommandException("No arena exist by that name!");
            }

            MirageSession session = sessions.getSession(MirageSession.class, PlayerUtil.checkPlayer(sender));
            session.vote(initFile);

            voting = true;

            ChatUtil.sendNotice(sender, "Your vote has been set to " + initFile + '.');
        }

        @Command(aliases = {"ignore"},
                usage = "<player[, player]>", desc = "Ignore a player",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.ignore")
        public void ignore(CommandContext args, CommandSender sender) throws CommandException {
            MirageSession session = sessions.getSession(MirageSession.class, PlayerUtil.checkPlayer(sender));
            String[] targets = args.getString(0).split(",");
            for (String target : targets) {
                session.ignore(target);
                ChatUtil.sendNotice(sender, "You will no longer be able to damage " + target + ".");
            }
        }

        @Command(aliases = {"unignore"},
                usage = "<player[, player]>", desc = "Unignore a player",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.unignore")
        public void unignore(CommandContext args, CommandSender sender) throws CommandException {
            MirageSession session = sessions.getSession(MirageSession.class, PlayerUtil.checkPlayer(sender));
            String[] targets = args.getString(0).split(",");
            for (String target : targets) {
                session.unignore(target);
                ChatUtil.sendNotice(sender, "You will now be able to damage " + target + ".");
            }
        }

        @Command(aliases = {"list"},
                usage = "[-p page] [prefix]", desc = "List all arena states",
                flags = "p:", min = 0, max = 1)
        @CommandPermissions("aurora.mirage.list")
        public void areaList(CommandContext args, CommandSender sender) throws CommandException {
            new PaginatedResult<File>(ChatColor.GOLD + "Arenas") {
                @Override
                public String format(File file) {
                    return file.getName();
                }
            }.display(
                    sender,
                    Arrays.asList(getWorkingDir().listFiles((dir, name)
                            -> (args.argsLength() < 1 || name.startsWith(args.getString(0)))
                            && new File(dir, name).isDirectory())),
                    args.getFlagInteger('p', 1)
            );
        }

        @Command(aliases = {"save"},
                usage = "<name>", desc = "Save an arena state",
                flags = "o", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.save")
        public void areaSave(CommandContext args, CommandSender sender) throws CommandException {
            BlockVector3 min = region.getMinimumPoint();

            String initFile = args.getString(0);
            File file = getFile(initFile);

            File directory = file.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            } else {
                if (!args.hasFlag('o')) {
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

            ChatUtil.sendNotice(sender, "Successfully saved.");
        }

        @Command(aliases = {"load"},
                usage = "<name>", desc = "Load an arena state",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.load")
        public void areaLoad(CommandContext args, CommandSender sender) throws CommandException {

            String initFile = args.getString(0);

            ChatUtil.sendNotice(sender, "Loading...");

            try {
                try {
                    changeMirage(initFile);
                } catch (FileNotFoundException ex) {
                    throw new CommandException("No arena state exist by that name!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                ChatUtil.sendError(sender, "Error encountered, check console.");
            }
        }
    }
}
