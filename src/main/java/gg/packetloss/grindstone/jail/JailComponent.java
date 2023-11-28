/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
import java.util.stream.Collectors;


@ComponentInformation(friendlyName = "Jail", desc = "Jail System")
@Depend(plugins = {"WorldEdit"}, components = {ChatBridgeComponent.class, GuildComponent.class})
public class JailComponent extends BukkitComponent implements Listener, Runnable {
    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private GuildComponent guilds;

    private InmateDatabase inmates;
    private JailCellDatabase jailCells;
    private LocalConfiguration config;
    private Map<Player, JailCell> cells = new HashMap<>();

    @Override
    public void enable() {

        //super.enable();
        config = configure(new LocalConfiguration());

        // Setup the inmates database
        File jailDirectory = new File(CommandBook.inst().getDataFolder().getPath() + "/jail");
        if (!jailDirectory.exists()) jailDirectory.mkdir();

        inmates = new CSVInmateDatabase(jailDirectory);
        jailCells = new CSVJailCellDatabase(jailDirectory);
        inmates.load();
        jailCells.load();

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            PrisonIdentifierConverter.register(registrar, this);

            registrar.register(JailCommandsRegistration.builder(), new JailCommands(this));
        });

        registerCommands(Commands.class);

        CommandBook.registerEvents(this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 20 * 2);
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

    public boolean isPrison(String prisonName) {
        return jailCells.prisonExist(prisonName);
    }

    public String getDefaultPrison() {
        return config.defaultJail;
    }

    public void broadcastJailing(CommandSender jailer, OfflinePlayer inmate, String reason) {
        if (!config.broadcastJails) {
            return;
        }

        String jailBroadcastMessage = jailer.getName() + " has jailed " + inmate.getName() +
                (reason.isEmpty() ? "." : " - " + reason + ".");
        ChatUtil.sendNotice(CommandBook.server().getOnlinePlayers(), jailBroadcastMessage);
        chatBridge.broadcast(jailBroadcastMessage);
    }

    public void broadcastUnjailing(CommandSender liberator, OfflinePlayer inmate, String reason) {
        if (!config.broadcastJails) {
            return;
        }

        String unjailBroadcastMessage = liberator.getName() + " has unjailed " + inmate.getName() +
                (reason.isEmpty() ? "." : " - " + reason + ".");
        ChatUtil.sendNotice(Bukkit.getOnlinePlayers(), unjailBroadcastMessage);
        chatBridge.broadcast(unjailBroadcastMessage);
    }

    public void jail(UUID ID, String source, String reason, long time, boolean mute) {
        jail(ID, config.defaultJail, source, reason, System.currentTimeMillis() + time, mute);
    }

    public void jail(UUID ID, String prison, CommandSender source, String reason, long end, boolean mute) {
        jail(ID, prison, source.getName(), reason, end, mute);
    }

    private String getDefaultReasonOrSpecified(String reason) {
        if (reason.isBlank()) {
            return  "unspecified reason";
        } else {
            return reason.trim().toLowerCase();
        }
    }

    public void jail(UUID ID, String prison, String source, String reason, long end, boolean mute) {
        reason = getDefaultReasonOrSpecified(reason);

        inmates.jail(ID, prison, source, reason, end, mute);

        chatBridge.modBroadcast(String.format(
                "%s jailed %s %s: %s",
                source,
                Bukkit.getOfflinePlayer(ID).getName(),
                TimeUtil.getPrettyEndDate(end),
                reason
        ));
    }

    public boolean unjail(UUID ID, CommandSender source, String reason) {
        return unjail(ID, source.getName(), reason);
    }

    public boolean unjail(UUID ID, String source, String reason) {
        reason = getDefaultReasonOrSpecified(reason);

        boolean unjailed = inmates.unjail(ID, source, reason);
        if (unjailed) {
            chatBridge.modBroadcast(String.format(
                    "%s unjailed %s: %s",
                    source,
                    Bukkit.getOfflinePlayer(ID).getName(),
                    reason
            ));
        }

        return unjailed;
    }

    public boolean checkSentence(Player player) {

        Inmate inmate = inmates.getInmate(player.getUniqueId());

        if (inmate != null) {
            if (inmate.getEnd() == 0L || inmate.getEnd() - System.currentTimeMillis() > 0) {
                return true;
            }
            unjail(player.getUniqueId(), "Auto Warden", "Temp-jail expired");
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
        builder.append(TimeUtil.getPrettyEndDate(inmate.getEnd()));
        if (reason != null) {
            builder.append(" for: ").append(reason);
        }
        builder.append(".");

        ChatUtil.sendWarning(player, builder.toString());
    }

    @Override
    public void run() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                if (isJailed(player)) {
                    JailCell cell = cells.get(player);
                    Inmate inmate = inmates.getInmate(player.getUniqueId());
                    if (cell == null || !cell.getPrisonName().equals(inmate.getPrisonName())) {
                        cell = assignCell(player, inmate.getPrisonName());
                    }

                    guilds.getState(player).ifPresent(GuildState::disablePowers);
                    player.setFoodLevel(5);

                    if (cell == null) {
                        player.kickPlayer("Unable to find a jail cell...");
                        CommandBook.logger().warning("Could not find a cell for the player: " + player.getName() + ".");
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

                    if (Bukkit.getMaxPlayers() - Bukkit.getOnlinePlayers().size() <= config.freeSpotsHeld) {
                        player.kickPlayer("You are not currently permitted to be online!");
                    }
                }
            } catch (Exception e) {
                player.kickPlayer("An error has occurred!");
                CommandBook.logger().warning("The Jail could not process the player: " + player.getName() + ".");
                CommandBook.logger().warning("Printing stack trace...");
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        cells.remove(player);
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
    public void onPrayerApplication(PrayerTriggerEvent event) {

        if (isJailed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    public class Commands {
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

                items = prisonCells.stream().map(cell -> cell.getCellName()
                        + " (" + cell.getWorldName() + "; " + cell.getX() + ", "
                        + cell.getY() + ", " + cell.getZ() + ")").collect(Collectors.toList());
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
