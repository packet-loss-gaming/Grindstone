package gg.packetloss.grindstone.warps;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.commandbook.util.entity.player.iterators.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.events.HomeTeleportEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static com.sk89q.commandbook.util.InputUtil.PlayerParser;
import static gg.packetloss.grindstone.items.custom.CustomItems.TOME_OF_THE_RIFT_SPLITTER;
import static gg.packetloss.grindstone.util.StringUtil.toTitleCase;

@ComponentInformation(
        friendlyName = "Rift Warps",
        desc = "Provides warps functionality"
)
public class WarpsComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private WarpManager warpManager;


    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        this.registerCommands(Commands.class);

        File warpsDirectory = new File(inst.getDataFolder().getPath() + "/warps");
        if (!warpsDirectory.exists()) warpsDirectory.mkdir();

        WarpDatabase warpDatabase = new CSVWarpDatabase("warps", warpsDirectory);
        warpDatabase.load();

        warpManager = new WarpManager(warpDatabase);
    }

    public Optional<Location> getRawBedLocation(Player player) {
        return warpManager.getHomeFor(player).map(WarpPoint::getLocation);
    }

    public Optional<Location> getBedLocation(Player player) {
        return warpManager.getHomeFor(player).map(WarpPoint::getSafeLocation);
    }

    public Location getRespawnLocation(Player player) {
        Location spawnLoc = player.getWorld().getSpawnLocation();
        return getBedLocation(player).orElse(spawnLoc);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        HomeTeleportEvent HTE = new HomeTeleportEvent(event.getPlayer(), getRespawnLocation(event.getPlayer()));
        server.getPluginManager().callEvent(HTE);
        if (!HTE.isCancelled()) event.setRespawnLocation(HTE.getDestination());
    }

    private boolean canSetPlayerBed(Location loc) {
        return !loc.getWorld().getName().toLowerCase().contains("legit");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Location bedLoc = event.getBed().getLocation();

        if (player == null || bedLoc == null) {
            return;
        }
        if (!canSetPlayerBed(bedLoc)) {
            return;
        }

        warpManager.setPlayerHomeAndNotify(player, bedLoc);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRightClickBed(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block.getType() != Material.BED_BLOCK) {
            return;
        }
        if (!canSetPlayerBed(block.getLocation()))  {
            return;
        }

        warpManager.setPlayerHomeAndNotify(player, block.getLocation());
    }

    public TextComponentChatPaginator<WarpPoint> getListResult() {
        return new TextComponentChatPaginator<WarpPoint>(ChatColor.GOLD, "Warps") {
            @Override
            public Optional<String> getPagerCommand(int page) {
                return Optional.of("/warps list -p " + page);
            }

            @Override
            public Text format(WarpPoint entry) {
                WarpQualifiedName qualifiedName = entry.getQualifiedName();

                String titleCaseName = toTitleCase(entry.getQualifiedName().getName());
                Text warpName = Text.of(
                        (qualifiedName.isGlobal() ? ChatColor.BLUE : ChatColor.DARK_BLUE),
                        qualifiedName.getDisplayName().toUpperCase(),
                        TextAction.Click.runCommand("/warp " + entry.getQualifiedName()),
                        TextAction.Hover.showText(Text.of("Teleport to ", titleCaseName))
                );

                return Text.of(warpName, ChatColor.YELLOW, " (World: ", Text.of(entry.getWorldName()), ")");
            }
        };
    }

    private class WarpNotFoundException extends CommandException {
        public WarpNotFoundException() {
            super("No warp could be found by that name!");
        }
    }

    public class Commands {
        @Command(
                aliases = {"warp"},
                usage = "<[qualifier:]warp> [targets...]",
                flags = "s",
                desc = "Teleport to a warp",
                min = 1,
                max = 2
        )
        public void warp(CommandContext args, CommandSender sender) throws CommandException {
            String[] parts = args.getString(0).split(":");

            // Parse the warp
            Optional<WarpPoint> optWarp;
            if (parts.length == 1) {
                optWarp = warpManager.lookupWarp(PlayerUtil.checkPlayer(sender), parts[0]);
            } else if (parts.length == 2) {
                optWarp = warpManager.lookupWarp(parts[0], parts[1]);
            } else {
                throw new CommandException("Invalid qualified warp name!");
            }

            if (optWarp.isEmpty()) {
                throw new WarpNotFoundException();
            }

            WarpPoint warp = optWarp.get();

            // Parse the targets
            Iterable<Player> targets;
            if (args.argsLength() == 2) {
                targets = PlayerParser.matchPlayers(sender, args.getString(1));
            } else {
                targets = Lists.newArrayList(PlayerUtil.checkPlayer(sender));
            }

            // Check access permissions if the sender is a player
            if (sender instanceof Player) {
                WarpQualifiedName qualifiedName = warp.getQualifiedName();
                UUID warpNamespace = qualifiedName.getNamespace();

                // Check warp access, pretend it doesn't exist if permission is denied
                if (((Player) sender).getUniqueId().equals(warpNamespace)) {
                    if (!inst.hasPermission(sender, "aurora.warp.access.self")) {
                        throw new WarpNotFoundException();
                    }
                } else if (qualifiedName.isGlobal()) {
                    if (!inst.hasPermission(sender, "aurora.warp.access.global")) {
                        throw new WarpNotFoundException();
                    }
                } else {
                    if (!inst.hasPermission(sender, "aurora.warp.access." + warpNamespace)) {
                        throw new WarpNotFoundException();
                    }
                }

                // Check teleport access on targets
                for (Player target : targets) {
                    if (target == sender) {
                        inst.checkPermission(sender, "aurora.warp.teleport.self");
                    } else {
                        inst.checkPermission(sender, "aurora.warp.teleport.other");
                    }
                }
            }

            Location loc = warp.getSafeLocation();
            (new TeleportPlayerIterator(sender, loc, args.hasFlag('s'))).iterate(targets);
        }

        @Command(aliases = {"home"}, desc = "Go to your home")
        public void teleportHome(CommandContext args, CommandSender sender) {
            sender.getServer().dispatchCommand(sender, "warp home");
        }

        @Command(
                aliases = {"warps"},
                desc = "Warp management"
        )
        @NestedCommand({ManagementCommands.class})
        public void warps(CommandContext args, CommandSender sender) throws CommandException {
        }
    }

    public class ManagementCommands {
        @Command(
                aliases = {"set"},
                usage = "<name>",
                flags = "g",
                desc = "Create/update a new warp",
                min = 1,
                max = 1
        )
        public void setWarp(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            WarpQualifiedName warpName;
            if (args.hasFlag('g')) {
                inst.checkPermission(sender, "aurora.warp.set.global");

                warpName = new WarpQualifiedName(args.getString(0));
            } else {
                inst.checkPermission(sender, "aurora.warp.set.self");

                if (!ItemUtil.removeItemOfName(player, CustomItemCenter.build(TOME_OF_THE_RIFT_SPLITTER), 1, false)) {
                    throw new CommandException("You need a Tome of the Rift Splitter to add or update a warp.");
                }

                warpName = new WarpQualifiedName(player.getUniqueId(), args.getString(0));
            }

            boolean isUpdate = warpManager.setWarp(warpName, player.getLocation()).isPresent();
            if (isUpdate) {
                ChatUtil.sendNotice(sender, "Warp '" + warpName.getDisplayName() + "' updated.");
            } else {
                ChatUtil.sendNotice(sender, "Warp '" + warpName.getDisplayName() + "' created.");
            }
        }

        @Command(
                aliases = {"destroy", "delete", "remove", "del", "des", "rem"},
                usage = "<name>",
                flags = "g",
                desc = "Remove a warp",
                min = 1,
                max = 1
        )
        public void removeWarp(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            WarpQualifiedName warpName;
            if (args.hasFlag('g')) {
                inst.checkPermission(sender, "aurora.warp.destroy.global");

                warpName = new WarpQualifiedName(args.getString(0));
            } else {
                inst.checkPermission(sender, "aurora.warp.destroy.self");

                warpName = new WarpQualifiedName(player.getUniqueId(), args.getString(0));
            }

            if (warpManager.destroyWarp(warpName)) {
                ChatUtil.sendNotice(sender, "Warp '" + warpName.getDisplayName() + "' destroyed.");
            } else {
                ChatUtil.sendNotice(sender, "Warp '" + warpName.getDisplayName() + "' not found.");
            }
        }

        @Command(
                aliases = {"list"},
                usage = "[-p page] [filter]",
                desc = "List warps",
                flags = "p:",
                min = 0,
                max = 1
        )
        @CommandPermissions({"aurora.warp.list"})
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {
            List<WarpPoint> warps = warpManager.getWarpsForPlayer(PlayerUtil.checkPlayer(sender));

            // Filter out unwanted warps
            if (args.argsLength() > 0) {
                String filter = args.getString(0).toUpperCase();
                warps.removeIf(warp -> !warp.getQualifiedName().getDisplayName().startsWith(filter));
            }

            // Sort warps for display
            warps.sort(Comparator.comparing(p -> p.getQualifiedName().isGlobal()));
            warps.sort(Comparator.comparing(p -> p.getQualifiedName().getDisplayName()));

            getListResult().display(sender, warps, args.getFlagInteger('p', 1));
        }
    }
}
