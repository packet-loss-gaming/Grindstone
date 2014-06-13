/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.MirageArena;

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
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@ComponentInformation(friendlyName = "Mirage Arena", desc = "What will you see next?")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class MirageArena extends AreaComponent<MirageArenaConfig> implements PersistentArena {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected SessionComponent sessions;

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

    public File getFile(String name) {
        return new File(getWorkingDir(), name + ".schematic");
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

        @Command(aliases = {"save"},
                usage = "<name>", desc = "Save an arena state",
                flags = "o", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.save")
        public void areaSave(CommandContext args, CommandSender sender) throws CommandException {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();
            Vector size = max.subtract(min).add(1, 1, 1);

            File file = getFile(args.getString(0));
            if (!args.hasFlag('o') && file.exists()) {
                throw new CommandException("An arena state by that name already exist!");
            }

            try {
                CuboidClipboard clipboard = new CuboidClipboard(size, min);
                clipboard.copy(new EditSession(new BukkitWorld(world), -1));
                SchematicFormat.MCEDIT.save(clipboard, file);
            } catch (IOException | DataException e) {
                throw new CommandException("That arena state could not be saved!");
            }
            ChatUtil.sendNotice(sender, "Successfully saved.");
        }

        @Command(aliases = {"load"},
                usage = "<name>", desc = "Load an arena state",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.mirage.load")
        public void areaLoad(CommandContext args, CommandSender sender) throws CommandException {

            File file = getFile(args.getString(0));

            if (!file.exists()) {
                throw new CommandException("No arena state exist by that name!");
            }

            try {
                SchematicFormat.MCEDIT.load(file).place(new EditSession(new BukkitWorld(world), -1), region.getMinimumPoint(), false);
            } catch (IOException | DataException | MaxChangedBlocksException e) {
                throw new CommandException("That arena state could not be loaded!");
            }
            ChatUtil.sendNotice(sender, "Successfully loaded.");
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
