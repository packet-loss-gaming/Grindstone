/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.jail;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.CollectionUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Jail", desc = "Jail System")
@Depend(plugins = {"WorldEdit"}, components = {AdminComponent.class})
public class JailComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;

    private InmateDatabase inmates;
    private JailCellDatabase jailCells;
    private LocalConfiguration config;
    private Map<Player, JailCell> cells = new HashMap<>();

    @Override
    public void enable() {

        //super.enable();
        config = configure(new LocalConfiguration());

        // Setup the inmates database
        File jailDirectory = new File(inst.getDataFolder().getPath() + "/jail");
        if (!jailDirectory.exists()) jailDirectory.mkdir();

        inmates = new CSVInmateDatabase(jailDirectory);
        jailCells = new CSVJailCellDatabase(jailDirectory);
        inmates.load();
        jailCells.load();
        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 20 * 2);
    }

    @Override
    public void reload() {

        super.reload();
        inmates.load();
        jailCells.load();
        configure(config);
    }

    @Override
    public void disable() {

        inmates.unload();
        jailCells.unload();
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("default-jail")
        public String defaultJail = "lava-flow";
        @Setting("jailed-message")
        public String jailMessage = "Your jail sentence does not permit this action!";
        @Setting("broadcast-jails")
        public boolean broadcastJails = true;
        @Setting("move-threshold")
        public int moveThreshold = 8;
        @Setting("free-spots-held")
        public int freeSpotsHeld = 2;
    }

    /**
     * Get the inmate database.
     *
     * @return Inmates
     */
    public InmateDatabase getInmateDatabase() {

        return inmates;
    }

    /**
     * Get the jailcell database.
     *
     * @return Jail cells
     */
    public JailCellDatabase getJailCellDatabase() {

        return jailCells;
    }

    public void jail(UUID ID, long time) {

        jail(ID, time, false);
    }

    public void jail(UUID ID, long time, boolean mute) {

        inmates.jail(ID, config.defaultJail, server.getConsoleSender(), "", System.currentTimeMillis() + time, mute);
    }

    public boolean checkSentence(Player player) {

        Inmate inmate = inmates.getInmate(player.getUniqueId());

        if (inmate != null) {
            if (inmate.getEnd() == 0L || inmate.getEnd() - System.currentTimeMillis() > 0) {
                return true;
            }
            inmates.unjail(player.getUniqueId(), null, "Temp-jail expired");
            inmates.save();
        }

        if (cells.containsKey(player)) {
            cells.remove(player);

            player.teleport(player.getWorld().getSpawnLocation());
            ChatUtil.sendNotice(player, "You have been unjailed.");
        }
        return false;
    }

    public boolean isJailed(Player player) {

        return checkSentence(player);
    }

    public boolean isJailed(UUID ID) {

        return inmates.isInmate(ID);
    }

    public boolean isJailMuted(Player player) {

        return checkSentence(player) && isJailMuted(player.getUniqueId());
    }

    public boolean isJailMuted(UUID ID) {

        return isJailed(ID) && inmates.getInmate(ID).isMuted();
    }

    public void notify(Player player) {

        ChatUtil.sendWarning(player, config.jailMessage);

        Inmate inmate = inmates.getInmate(player.getUniqueId());
        String reason = inmate.getReason();

        StringBuilder builder = new StringBuilder();
        builder.append("Jailed ");

        // Date
        if (inmate.getEnd() != 0) {
            builder.append("till: ");

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(inmate.getEnd());

            builder.append(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US));
            builder.append(" ");
            builder.append(calendar.get(Calendar.DAY_OF_MONTH));
            builder.append(" ");
            builder.append(calendar.get(Calendar.YEAR));
            builder.append(" at ");
            builder.append(calendar.get(Calendar.HOUR));
            builder.append(":");
            builder.append(calendar.get(Calendar.MINUTE));
            builder.append(calendar.get(Calendar.AM_PM) == 0 ? "AM" : "PM");
        } else {
            builder.append("indefinitely");
        }

        if (reason != null) {
            builder.append(" for: ").append(reason);
        }
        builder.append(".");

        ChatUtil.sendWarning(player, builder.toString());
    }

    @Override
    public void run() {

        for (Player player : server.getOnlinePlayers()) {
            try {

                if (isJailed(player)) {

                    JailCell cell = cells.get(player);
                    Inmate inmate = inmates.getInmate(player.getUniqueId());
                    if (cell == null || !cell.getPrisonName().equals(inmate.getPrisonName())) {
                        cell = assignCell(player, inmate.getPrisonName());
                    }

                    adminComponent.standardizePlayer(player, true);
                    player.setFoodLevel(5);

                    if (cell == null) {
                        player.kickPlayer("Unable to find a jail cell...");
                        log.warning("Could not find a cell for the player: " + player.getName() + ".");
                        continue;
                    }

                    Location loc = player.getLocation();
                    Location cellLoc = cell.getLocation();
                    if (player.getWorld() != cellLoc.getWorld() || loc.distanceSquared(cellLoc) > (config.moveThreshold * config.moveThreshold)) {
                        Entity v = player.getVehicle();
                        if (v != null) {
                            v.eject();
                        }
                        player.teleport(cell.getLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    }

                    if (server.getMaxPlayers() - server.getOnlinePlayers().size() <= config.freeSpotsHeld) {
                        player.kickPlayer("You are not currently permitted to be online!");
                    }
                }
            } catch (Exception e) {
                player.kickPlayer("An error has occurred!");
                log.warning("The Jail could not process the player: " + player.getName() + ".");
                log.warning("Printing stack trace...");
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (cells.containsKey(player)) {
            cells.remove(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        if (isJailed(player)) {
            notify(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        if (isJailed(player)) {
            notify(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();
        if (entity instanceof Player && isJailed((Player) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        if (isJailed(player)) {
            notify(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        if (isJailMuted(player)) {
            notify(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (isJailed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    public class Commands {

        @Command(aliases = {"jail"}, usage = "[-t end] <target> [prison] [reason...]",
                desc = "Jail a player", flags = "mset:o", min = 1, max = -1)
        @CommandPermissions({"aurora.jail.jail"})
        public void jailCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player inmate = null;
            UUID inmateID = null;
            String prisonName = args.argsLength() >= 2 ? args.getString(1) : config.defaultJail;
            long endDate = args.hasFlag('t') ? InputUtil.TimeParser.matchFutureDate(args.getFlag('t')) : 0L;
            String message = args.argsLength() >= 3 ? args.getJoinedStrings(2) : "";

            final boolean hasExemptOverride = args.hasFlag('o') && inst.hasPermission(sender, "aurora.jail.exempt.override");

            String inmateName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            // Check if it's a player in the server right now
            try {
                // Exact mode matches names exactly
                if (args.hasFlag('e')) {
                    inmate = InputUtil.PlayerParser.matchPlayerExactly(sender, inmateName);
                } else {
                    inmate = InputUtil.PlayerParser.matchSinglePlayer(sender, inmateName);
                }

                inmateID = inmate.getUniqueId();

                // They are offline
            } catch (CommandException e) {
                OfflinePlayer player = server.getOfflinePlayer(inmateName);
                inmateID = player == null ? null : player.getUniqueId();
            }

            if (!hasExemptOverride) {
                try {
                    if (inst.hasPermission(inmate, "aurora.jail.exempt")) {
                        if (inst.hasPermission(sender, "aurora.jail.exempt.override")) {
                            throw new CommandException("That player is exempt from being jailed! (use -o flag to override this)");
                        } else {
                            throw new CommandException("That player is exempt from being jailed!");
                        }
                    }
                } catch (NullPointerException npe) {
                    if (inst.hasPermission(sender, "aurora.jail.exempt.override")) {
                        throw new CommandException("That player is offline, and cannot be jailed! (use -o flag to override this)");
                    } else {
                        throw new CommandException("That player is offline, and cannot be jailed!");
                    }
                }
            }

            if (!jailCells.prisonExist(prisonName)) throw new CommandException("No such prison exists.");

            if (inmateID == null) {
                throw new CommandException("That player could not be jailed!");
            }

            // Jail the player
            inmates.jail(inmateID, prisonName, sender, message, endDate, args.hasFlag('m'));

            // Tell the sender of their success
            ChatUtil.sendNotice(sender, "The player: " + inmateName + " has been jailed!");

            if (!inmates.save()) {
                throw new CommandException("Inmate database failed to save. See console.");
            }

            // Broadcast the Message
            if (config.broadcastJails && !args.hasFlag('s')) {
                ChatUtil.sendNotice(server.getOnlinePlayers(), sender.getName() + " has jailed "
                        + inmateName + (message.isEmpty() ? "!" : " - " + message + "."));
            }
        }

        @Command(aliases = {"unjail"}, usage = "<target> [reason...]", desc = "Unjail a player", min = 1, max = -1)
        @CommandPermissions({"aurora.jail.unjail"})
        public void unjailCmd(CommandContext args, CommandSender sender) throws CommandException {

            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1) : "";

            String inmateName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            UUID ID = CommandBook.server().getOfflinePlayer(inmateName).getUniqueId();

            if (inmates.unjail(ID, sender, message)) {
                ChatUtil.sendNotice(sender, inmateName + " unjailed.");

                if (!inmates.save()) {
                    throw new CommandException("Inmate database failed to save. See console.");
                }
            } else {
                ChatUtil.sendError(sender, inmateName + " was not jailed.");
            }
        }

        @Command(aliases = {"cells"}, desc = "Jail Cell management")
        @NestedCommand({ManagementCommands.class})
        public void cellCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class ManagementCommands {

        @Command(aliases = {"add"}, usage = "<prison> [name]", desc = "Create a cell", min = 1, max = 2)
        @CommandPermissions({"aurora.jail.cells.add"})
        public void addCellCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            String prisonName = args.getString(0);
            String cellName = args.argsLength() > 1 ? args.getString(1) : String.valueOf(System.currentTimeMillis());
            Location loc = player.getLocation();

            if (jailCells.cellExist(prisonName, cellName)) {
                throw new CommandException("Cell already exists!");
            }

            jailCells.createJailCell(prisonName, cellName, player, loc);

            ChatUtil.sendNotice(sender, "Cell '" + cellName + "' created.");

            if (!jailCells.save()) {
                throw new CommandException("Inmate database failed to save. See console.");
            }
        }

        @Command(aliases = "list", usage = "[prison]", desc = "List cells",
                flags = "p:", min = 0, max = 1)
        @CommandPermissions("aurora.jail.cells.list")
        public void listCellCmd(CommandContext args, CommandSender sender) throws CommandException {

            String prison = args.argsLength() < 1 ? null : args.getString(0);

            List<String> items;
            if (prison == null) {
                items = jailCells.getPrisons();
            } else {
                List<JailCell> prisonCells = jailCells.getPrison(prison);

                if (prisonCells == null) {
                    throw new CommandException("No such prison exist!");
                }

                items = new ArrayList<>();

                items.addAll(prisonCells.stream().map(cell -> cell.getCellName()
                        + " (" + cell.getWorldName() + "; " + cell.getX() + ", "
                        + cell.getY() + ", " + cell.getZ() + ")").collect(Collectors.toList()));
            }

            Collections.sort(items);

            final int entryToShow = 9;
            final int listSize = items.size();

            // Page info
            int page = 0;
            int maxPage = listSize / entryToShow;

            if (args.hasFlag('p')) {
                page = Math.min(maxPage, Math.max(0, args.getFlagInteger('p') - 1));
            }

            // Viewable record info
            int min = entryToShow * page;
            int max = Math.min(listSize, min + entryToShow);

            String type = prison == null ? "Prison" : "Cell";
            ChatUtil.sendNotice(sender, ChatColor.GOLD,
                    type + " List - Page (" + Math.min(maxPage + 1, page + 1) + "/" + (maxPage + 1) + ")");

            for (int i = min; i < max; i++) {
                ChatUtil.sendNotice(sender, items.get(i));
            }
        }

        @Command(aliases = {"del", "delete", "remove", "rem"}, usage = "<prison> <cell>",
                desc = "Remove a cell", min = 2, max = 2)
        @CommandPermissions({"aurora.jail.cells.remove"})
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {

            String prisonName = args.getString(0);
            String cellName = args.getString(1);

            if (!jailCells.deleteJailCell(prisonName, cellName, sender) || !jailCells.save()) {
                throw new CommandException("No such cell could be successfully found/removed in that prison!");
            }
            ChatUtil.sendNotice(sender, "Cell '" + cellName + "' deleted.");
        }
    }

    private JailCell assignCell(Player player, String prisonName) {

        JailCell jailCell;
        List<JailCell> prison = jailCells.getPrison(prisonName);

        prison = prison == null ? jailCells.getPrison(config.defaultJail) : prison;

        if (prison != null && prison.size() > 0) {
            jailCell = CollectionUtil.getElement(prison);
        } else {
            jailCell = null;
        }

        cells.put(player, jailCell);

        return jailCell;
    }
}
