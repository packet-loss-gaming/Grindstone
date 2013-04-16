package com.skelril.aurora.jail;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
    private Map<Player, JailCell> cell = new HashMap<>();
    private static final int MOVE_THRESHOLD = 8;
    private static final int MOVE_THRESHOLD_SQ = MOVE_THRESHOLD * MOVE_THRESHOLD;

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
        getInmateDatabase().load();
        getJailCellDatabase().load();
        configure(config);
    }

    @Override
    public void disable() {

        inmates.unload();
        jailCells.unload();
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("message")
        public String jailMessage = "You have been jailed";
        @Setting("broadcast-jails")
        public boolean broadcastJails = true;
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

    public void jail(String name, long time) {

        getInmateDatabase().jail(name, "lava-flow", server.getConsoleSender(), "", System.currentTimeMillis() + time);
    }

    public boolean isJailed(Player player) {

        return isJailed(player.getName());
    }

    public boolean isJailed(String name) {

        return getInmateDatabase().isJailedName(name);
    }

    @Override
    public void run() {

        for (Player player : server.getOnlinePlayers()) {
            try {

                if (getInmateDatabase().isJailedName(player.getName())) {

                    if (!player.isOnline() && cell.containsKey(player)) {
                        cell.remove(player);
                        continue;
                    }

                    if (player.isOnline() && !cell.containsKey(player)) {
                        Inmate inmate = getInmateDatabase().getJailedName(player.getName());
                        assignCell(player, inmate.getPrisonName());
                    }

                    adminComponent.standardizePlayer(player, true);
                    player.setFoodLevel(5);

                    Location loc = player.getLocation();
                    Location cellLoc = cell.get(player).getLocation();
                    if (player.getWorld() != cellLoc.getWorld()
                            || loc.distanceSquared(cellLoc) > MOVE_THRESHOLD_SQ) {
                        player.teleport(cell.get(player).getLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    }
                }


            } catch (Exception e) {
                log.warning("Could not find a cell for the player: " + player.getName() + ".");
                PlayerKickEvent playerKickEvent = new PlayerKickEvent(player, "Jail", "Kicked!");
                if (!playerKickEvent.isCancelled()) {
                    Bukkit.broadcastMessage("The player: " + player.getName() + " has been kicked.");
                    player.kickPlayer("Kicked!");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        if (isJailed(player)) {
            ChatUtil.sendWarning(player, "You are jailed!");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        if (isJailed(player)) {
            ChatUtil.sendWarning(player, "You are jailed!");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        if (isJailed(player)) {
            ChatUtil.sendWarning(player, "You are jailed!");
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

        @Command(aliases = {"jail"}, usage = "[-t end] <target> <prison> [reason...]",
                desc = "Jail a player", flags = "set:o", min = 2, max = -1)
        @CommandPermissions({"aurora.jail.jail"})
        public void jailCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player inmate = null;
            String inmateName = "";
            String prisonName = args.getString(1);
            long endDate = args.hasFlag('t') ? CommandBookUtil.matchFutureDate(args.getFlag('t')) : 0L;
            String message = args.argsLength() >= 3 ? args.getJoinedStrings(1)
                                                    : "Jailed!";

            final boolean hasExemptOverride = args.hasFlag('o')
                    && inst.hasPermission(sender, "aurora.jail.exempt.override");

            // Check if it's a player in the server right now
            try {


                // Exact mode matches names exactly
                if (args.hasFlag('e')) {
                    inmate = PlayerUtil.matchPlayerExactly(sender, args.getString(0));
                } else {
                    inmate = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                }

                inmateName = inmate.getName();

                // They are offline
            } catch (CommandException e) {
                inmateName = args.getString(0)
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("\0", "")
                        .replace("\b", "");
            }

            if (!hasExemptOverride) {
                try {
                    if (inst.hasPermission(inmate, "aurora.jail.exempt")) {
                        if (inst.hasPermission(sender, "aurora.jail.exempt.override")) {
                            throw new CommandException("The player: " + inmateName
                                    + " is exempt from being jailed! " +
                                    "(use -o flag to override this)");
                        } else {
                            throw new CommandException("The player: " + inmateName
                                    + " is exempt from being jailed!");
                        }
                    }
                } catch (NullPointerException npe) {
                    if (inst.hasPermission(sender, "aurora.jail.exempt.override")) {
                        throw new CommandException("The player: " + inmateName + " is offline, " +
                                "and cannot be jailed! (use -o flag to override this)");
                    } else {
                        throw new CommandException("The player: " + inmateName + " is offline, " +
                                "and cannot be jailed!");
                    }
                }
            }

            if (!prisonExist(prisonName))
                throw new CommandException("No such prison exists.");


            // Jail the player
            getInmateDatabase().jail(inmateName, prisonName, sender, message, endDate);

            // Tell the sender of their success
            ChatUtil.sendNotice(sender, "The player: " + inmateName + " has been jailed!");

            // Broadcast the Message
            if (config.broadcastJails && !args.hasFlag('s')) {
                for (Player player : server.getOnlinePlayers()) {
                    if (inmateName.equalsIgnoreCase(player.getName())
                            || !(sender instanceof Player)
                            || player.getName().equalsIgnoreCase(sender.getName()))
                        continue;
                    ChatUtil.sendNotice(player, PlayerUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " has jailed " + inmateName + " - " + message);
                }
            }

            if (!getInmateDatabase().save()) {
                ChatUtil.sendError(sender, "Inmate database failed to save. See console.");
            }
        }

        @Command(aliases = {"unjail"}, usage = "<target> [reason...]", desc = "Unjail a player", min = 1, max = -1)
        @CommandPermissions({"aurora.jail.unjail"})
        public void unjailCmd(CommandContext args, CommandSender sender) throws CommandException {

            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                                                    : "Unjailed!";

            String inmateName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            if (getInmateDatabase().unjail(inmateName, sender, message)) {
                ChatUtil.sendNotice(sender, inmateName + " unjailed.");

                if (!getInmateDatabase().save()) {
                    sender.sendMessage(ChatColor.RED + "Jail database failed to save. See console.");
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

        @Command(aliases = {"addcell"}, usage = "<name> <prison>", desc = "Create a cell", min = 2, max = 2)
        @CommandPermissions({"aurora.jail.cells.add"})
        public void addCellCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String cellName = args.getString(0);
                String prisonName = args.getString(1);
                Player player = (Player) sender;
                Location loc = player.getLocation();

                if (getJailCellDatabase().cellExist(cellName)) {
                    throw new CommandException("Cell already exists!");
                }

                getJailCellDatabase().createJailCell(cellName, prisonName, player, loc);

                sender.sendMessage(ChatColor.YELLOW + "Cell '" + cellName + "' created.");

                if (!getJailCellDatabase().save()) {
                    ChatUtil.sendError(sender, "Inmate database failed to save. See console.");
                }
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"del", "delete", "remove", "rem"}, usage = "<name>",
                desc = "Remove a cell", min = 1, max = 2)
        @CommandPermissions({"aurora.jail.cells.remove"})
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {

            String cellName = args.getString(0);
            getJailCellDatabase().deleteJailCell(cellName, sender);
            sender.sendMessage(ChatColor.YELLOW + "Cell '" + cellName + "' deleted.");

        }
    }

    private void assignCell(Player player, String prisonName) {

        List<JailCell> prison = new ArrayList<>();
        for (JailCell jailCell : getJailCellDatabase().getJailCells()) {
            if (jailCell.getPrisonName().equalsIgnoreCase(prisonName))
                prison.add(jailCell);
        }

        if (prison.size() > 1) {
            JailCell cell = prison.get(ChanceUtil.getRandom(prison.size() - 1));
            this.cell.put(player, cell);
        } else {
            JailCell cell = prison.get(0);
            this.cell.put(player, cell);
        }
    }

    private boolean prisonExist(String prisonName) {

        for (JailCell jailCell : getJailCellDatabase().getJailCells()) {
            if (jailCell.getPrisonName().equalsIgnoreCase(prisonName)) {
                return true;
            }
        }
        return false;
    }
}
