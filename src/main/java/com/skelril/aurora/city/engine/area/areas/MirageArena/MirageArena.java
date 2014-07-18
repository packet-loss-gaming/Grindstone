/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.MirageArena;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.area.AreaComponent;
import com.skelril.aurora.city.engine.area.PersistentArena;
import com.skelril.aurora.city.engine.pvp.PvPComponent;
import com.skelril.aurora.city.engine.pvp.PvPScope;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.util.APIUtil;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import com.skelril.aurora.util.timer.TimerUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@ComponentInformation(friendlyName = "Mirage Arena", desc = "What will you see next?")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class MirageArena extends AreaComponent<MirageArenaConfig> implements PersistentArena {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected SessionComponent sessions;

    protected int ticks = 0;
    protected PvPScope scope;
    protected boolean editing = false;
    protected HashMap<String, PlayerState> playerState = new HashMap<>();

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            region = WG.getRegionManager(world).getRegion("carpe-diem-district-mirage-arena");
            tick = 5 * 20;
            listener = new MirageArenaListener(this);
            config = new MirageArenaConfig();
            reloadData();

            registerScope();
            registerCommands(Commands.class);
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
        if (!isEmpty()) {
            equalize();
            fakeXPGain();
            shiftMirage();
        }
        writeData(true);
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

    public void fakeXPGain() {
        for (Player player : getContained(Player.class)) {
            for (int i = ChanceUtil.getRandom(5); i > 0; --i) {
                server.getPluginManager().callEvent(new PlayerExpChangeEvent(player,
                        ChanceUtil.getRandom(config.fakeXP)));
            }
        }
    }

    public void shiftMirage() {
        ++ticks;
        if (ticks >= 60) {
            String next = getNextMirage(true);
            if (next == null) return;
            ticks = 0;
            Collection<Player> players = getContained(Player.class);
            try {
                ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, "Attempting to change the current mirage to " + next + "...");
                changeMirage(next);
            } catch (CommandException e) {
                ChatUtil.sendError(players, "The arena was already changing, the vote has been cancelled!");
            } catch (IOException | DataException e) {
                e.printStackTrace();
                ChatUtil.sendError(players, "The arena could not be changed to " + next + "!");
            }
        } else if (ticks % 10 == 0) {
            String next = getNextMirage(false);
            if (next == null) return;
            Collection<Player> players = getContained(Player.class);
            ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, "The currently winning mirage is " + next + '.');
            ChatUtil.sendNotice(players, ChatColor.DARK_AQUA, ((60 - ticks) * 5) + " seconds til arena change.");
        }
    }

    public String getNextMirage(boolean clearOldVotes) {
        Map<String, ArenaVote> votes = new HashMap<>();
        getContained(Player.class).stream().forEach(p -> {
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
        Collections.sort(results, (o1, o2) -> o2.getVotes() - o1.getVotes());
        return results.isEmpty() ? null : results.get(0).getArena();
    }

    public void clearItems() {
        IntegratedRunnable normal = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                if (TimerUtil.matchesFilter(times, 10, 5)) {
                    ChatUtil.sendWarning(getContained(Player.class), "Clearing all arena items in: " + times + " seconds.");
                }
                return true;
            }
            @Override
            public void end() {
                ChatUtil.sendWarning(getContained(Player.class), "Clearing arena items!");
                for (Item item : getContained(Item.class)) {
                    item.remove();
                }
            }
        };
        TimedRunnable timed = new TimedRunnable(normal, 30);
        BukkitTask task = server.getScheduler().runTaskTimer(inst, timed, 0, 20);
        timed.setTask(task);
    }

    public void freePlayers() {
        for (Player player : getContained(Player.class)) {
            LocationUtil.toGround(player);
        }
    }

    public void callEdit(EditSession editor, CuboidClipboard board,
                         int cx, int cy, int maxX, int maxY, int maxZ) {

        if (cy >= maxY) {
            ChatUtil.sendNotice(getContained(Player.class), "Editing Completed.");
            editing = false;
            resendChunks();
            freePlayers();
            return;
        } else if (cx == 0 && cy % 10 == 0) {
            ChatUtil.sendNotice(getContained(Player.class), "Editing Layer: " + cy + '/' + maxY);
        }

        long start = System.currentTimeMillis();

        edit:
        {
            for (int x = cx; x < maxX; ++x) {
                for (int z = 0; z < maxZ; ++z) {
                    Vector v = new Vector(x, cy, z);
                    Vector target = v.add(region.getMinimumPoint());
                    BaseBlock targetBlock = board.getBlock(v);
                    try {
                        editor.setBlock(target, targetBlock);
                    } catch (MaxChangedBlocksException e) {
                        e.printStackTrace();
                    }
                }
                if (System.currentTimeMillis() - start >= 100) {
                    cx = x;
                    break edit;
                }
            }
            cx = 0;
            cy++;
        }

        long post = System.currentTimeMillis() - start;

        final int finalCy = cy;
        final int finalCx = cx;
        server.getScheduler().runTaskLater(inst, () -> {
            callEdit(editor, board, finalCx, finalCy, maxX, maxY, maxZ);
        }, post / 5);
    }

    public void changeMirage(String newMirage) throws IOException, DataException, CommandException {
        File file = getFile(newMirage);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }

        if (editing) {
            throw new CommandException("Editing is already in progress!");
        }
        editing = true;

        EditSession editor = new EditSession(new BukkitWorld(world), -1);
        editor.setFastMode(true);
        CuboidClipboard clipboard = SchematicFormat.MCEDIT.load(file);
        int maxX = clipboard.getWidth();
        int maxY = clipboard.getHeight();
        int maxZ = clipboard.getLength();

        clearItems();
        callEdit(editor, clipboard, 0, 0, maxX, maxY, maxZ);
    }

    public void resendChunks() {

        com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
        com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        for (int x = minX; x < maxX; x += 16) {
            for (int z = minZ; z < maxZ; z += 16) {
                world.refreshChunk(x / 16, z / 16);
            }
        }
    }

    public File getFile(String name) {
        return new File(getWorkingDir().getPath() + '/' + name + '/' + "arena.schematic");
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
            Vector min = region.getMinimumPoint();

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

            try {
                EditSession editor = new EditSession(new BukkitWorld(world), -1);
                Vector size = region.getMaximumPoint().subtract(min).add(1, 1, 1);

                CuboidClipboard clipboard = new CuboidClipboard(size, min);
                clipboard.copy(editor);
                SchematicFormat.MCEDIT.save(clipboard, getFile(initFile));

            } catch (IOException | DataException e) {
                e.printStackTrace();
                throw new CommandException("That arena state could not be saved!");
            }
            ChatUtil.sendNotice(sender, "Successfully saved.");
        }

        @Command(aliases = {"cleardrops", "dc"},
                usage = "", desc = "Clear drops in the arena",
                flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.mirage.cleardrops")
        public void areaDC(CommandContext args, CommandSender sender) throws CommandException {
            clearItems();
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
            } catch (IOException | DataException e) {
                e.printStackTrace();
                ChatUtil.sendError(sender, "Error encountered, check console.");
            }
        }
    }

    private void equalize() {
        for (Player player : getContained(Player.class)) {
            try {
                admin.deadmin(player);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public void writeData(boolean doAsync) {
        Runnable run = () -> {
            respawnsFile:
            {
                File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
                if (playerStateFile.exists()) {
                    Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);

                    if (playerState.equals(playerStateFileO)) {
                        break respawnsFile;
                    }
                }
                IOUtil.toBinaryFile(getWorkingDir(), "respawns", playerState);
            }
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
                log.info("Loaded: " + playerState.size() + " respawn records for the Mirage Arena.");
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
                        log.info("Loaded: " + playerState.size() + " respawn records for the Mirage Arena.");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
    }
}
