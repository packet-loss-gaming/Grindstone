/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.MirageArena;

import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.area.AreaComponent;
import com.skelril.aurora.city.engine.area.PersistentArena;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.util.APIUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

@ComponentInformation(friendlyName = "Mirage Arena", desc = "What will you see next?")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class MirageArena extends AreaComponent<MirageArenaConfig> implements PersistentArena {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected SessionComponent sessions;

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
        }
        writeData(true);
    }

    public void freePlayers() {
        for (Player player : getContained(Player.class)) {
            LocationUtil.toGround(player);
        }
    }

    public File getFile(String name, int number) {
        return new File(getWorkingDir().getPath() + '/' + name + '/' + name + '-' + number + ".schematic");
    }

    public class Commands {
        @Command(aliases = {"mirage"}, desc = "Mirage Commands")
        @NestedCommand({MirageCommands.class})
        public void profileCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class MirageCommands {

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
                usage = "[prefix]", desc = "List all arena states",
                flags = "", min = 0, max = 1)
        @CommandPermissions("aurora.mirage.list")
        public void areaList(CommandContext args, CommandSender sender) throws CommandException {
            new PaginatedResult<File>("Arenas") {
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
            File file = getFile(initFile, 0);

            File directory = file.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            } else {
                if (!args.hasFlag('o')) {
                    throw new CommandException("An arena state by that name already exist!");
                } else {
                    for (File aFile : directory.listFiles((dir, name) -> name.endsWith(".schematic"))) {
                        aFile.delete();
                    }
                }
            }

            try {
                EditSession editor = new EditSession(new BukkitWorld(world), -1);
                int diff = region.getMaximumPoint().getBlockY() - min.getBlockY();
                for (int base = 0; base < diff; base += 4) {
                    Vector origin = min.add(0, base, 0);
                    Vector size = region.getMaximumPoint().subtract(origin).setY(Math.min(diff - base, 4)).add(1, 1, 1);

                    CuboidClipboard clipboard = new CuboidClipboard(size, origin);
                    clipboard.copy(editor);
                    SchematicFormat.MCEDIT.save(clipboard, getFile(initFile, base / 4));
                }
            } catch (IOException | DataException e) {
                e.printStackTrace();
                throw new CommandException("That arena state could not be saved!");
            }
            ChatUtil.sendNotice(sender, "Successfully saved.");
        }

        @Command(aliases = {"load"},
                usage = "[parts] <name>", desc = "Load an arena state",
                flags = "", min = 1, max = 2)
        @CommandPermissions("aurora.mirage.load")
        public void areaLoad(CommandContext args, CommandSender sender) throws CommandException {

            final int parts;
            String initFile;
            if (args.argsLength() > 1) {
                parts = args.getInteger(0);
                initFile = args.getString(1);
            } else {
                parts = 4;
                initFile = args.getString(0);
            }
            File file = getFile(initFile, 0);

            if (!file.exists()) {
                throw new CommandException("No arena state exist by that name!");
            }

            editing = true;

            double start = System.currentTimeMillis();
            int files = file.getParentFile().listFiles((dir, name) -> name.endsWith(".schematic")).length - 1;
            EditSession editor = new EditSession(new BukkitWorld(world), -1);
            for (int base = 0; file.exists() || base <= files; ++base) {

                final File finalFile = file;
                final int finalBase = base;

                server.getScheduler().runTaskLater(inst, () -> {
                    try {
                        SchematicFormat.MCEDIT.load(finalFile).place(
                                editor,
                                region.getMinimumPoint().add(0, finalBase * parts, 0),
                                false
                        );
                    } catch (MaxChangedBlocksException | IOException | DataException e) {
                        e.printStackTrace();
                        ChatUtil.sendError(sender, "Error encountered, check console.");
                    }

                    if (finalBase == files) {
                        editing = false;
                        freePlayers();

                        double elapsed = System.currentTimeMillis() - start;
                        double workTime = elapsed - (250 * finalBase);
                        ChatUtil.sendNotice(sender, "Done loading.");
                        ChatUtil.sendNotice(sender, " - Editing: " + workTime + "ms");
                        ChatUtil.sendNotice(sender, " - Total: " + elapsed + "ms");
                    }
                }, 5 * base);

                file = getFile(initFile, base + 1);
            }
            ChatUtil.sendNotice(sender, "Loading...");
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

    public boolean canFight(Player attacker, Player defender) {
        return contains(attacker)
            && contains(defender)
            && !sessions.getSession(MirageSession.class, attacker).isIgnored(defender.getName());
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
