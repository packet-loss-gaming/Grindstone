package gg.packetloss.grindstone.city.engine.pixieitems;

import com.google.common.collect.ImmutableSet;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.city.engine.pixieitems.manager.NewSinkResult;
import gg.packetloss.grindstone.city.engine.pixieitems.manager.NewSourceResult;
import gg.packetloss.grindstone.city.engine.pixieitems.manager.PixieNetworkManager;
import gg.packetloss.grindstone.city.engine.pixieitems.manager.ThreadedPixieNetworkManager;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.EnvironmentUtil.isChest;

@ComponentInformation(friendlyName = "Item Sorter", desc = "A system of magical pixie item sorting.")
@Depend(components = {SessionComponent.class})
public class ItemSorterComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;

    private PixieNetworkManager manager = new ThreadedPixieNetworkManager();

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        registerCommands(Commands.class);
    }

    private boolean shouldConsider(Block block) {
        if (!isChest(block)) {
            return false;
        }

        if (!block.getWorld().getName().equals("City")) {
            return false;
        }

        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        PixieCommandSession session = sessions.getSession(PixieCommandSession.class, player);
        if (session.getCurrentCommand() == PixieCommand.NOTHING) {
            return;
        }

        Block block = event.getClickedBlock();

        if (!shouldConsider(block)) {
            return;
        }

        // We should always have a network ID if a command action is set.
        int networkID = session.getCurrentNetworkID().get();
        switch (session.getCurrentCommand()) {
            case ADD_SOURCE:
                manager.addSource(networkID, block).thenAccept((optResult) -> {
                    if (optResult.isEmpty()) {
                        ChatUtil.sendError(player, "An error occurred while attempting to create this source.");
                        return;
                    }

                    NewSourceResult result = optResult.get();
                    if (result.isNew()) {
                        ChatUtil.sendNotice(player, "Chest updated to source!");
                    } else {
                        ChatUtil.sendError(player, "Chest is already a source!");
                    }
                });
                break;
            case ADD_SINK:
                manager.addSink(networkID, block).thenAccept((optResult) -> {
                    if (optResult.isEmpty()) {
                        ChatUtil.sendError(player, "An error occurred while attempting to create this sink.");
                        return;
                    }

                    NewSinkResult result = optResult.get();
                    ChatUtil.sendNotice(player, "Chest updated to sink! Accepts:");

                    ImmutableSet<String> sinkItems = result.getItemNames();
                    if (sinkItems.isEmpty()) {
                        ChatUtil.sendNotice(player, " - " + ChatColor.BLUE + "ANYTHING");
                    } else {
                        for (String sinkItem : sinkItems) {
                            ChatUtil.sendNotice(player, " - " + ChatColor.BLUE + sinkItem.toUpperCase());
                        }
                    }
                });
                break;
        }

        session.performedAction();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (!shouldConsider(block)) {
            return;
        }

        manager.maybeExpandChest(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!shouldConsider(block)) {
            return;
        }

        manager.removeChest(block.getLocation());
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.CHEST) {
           return;
        }

        Inventory inventory = event.getInventory();
        server.getScheduler().runTaskLater(inst, () -> {
            if (!inventory.getViewers().isEmpty()) {
                return;
            }

            Optional<Integer> optNetworkID;

            InventoryHolder holder = inventory.getHolder();
            if (holder instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest) holder;
                Chest leftChest = (Chest) doubleChest.getLeftSide();
                Chest rightChest = (Chest) doubleChest.getRightSide();

                optNetworkID = manager.getNetworkFromSourceChest(leftChest.getBlock(), rightChest.getBlock());
            } else {
                Chest chest = (Chest) holder;
                optNetworkID = manager.getNetworkFromSourceChest(chest.getBlock());
            }

            if (optNetworkID.isEmpty()) {
                return;
            }

            int networkID = optNetworkID.get();
            manager.sourceItems(networkID, event.getInventory());
        }, 1);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        manager.handleChunkLoad(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        manager.handleChunkUnload(event.getChunk());
    }

    public class Commands {
        @Command(aliases = {"/sorter"}, desc = "Item sorter system management")
        @CommandPermissions({"aurora.itemsorter"})
        @NestedCommand({SorterCommands.class})
        public void subCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class SorterCommands {
        @Command(aliases = {"create"},
                 usage = "<network name>", desc = "Create a new sorter system", min = 1, max = 1)
        public void createCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);
            String name = args.getString(0).toUpperCase();

            manager.createNetwork(owner.getUniqueId(), name).thenAccept((optNetworkID) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optNetworkID.isEmpty()) {
                        ChatUtil.sendError(sender, "Failed to create network!");
                        return;
                    }

                    ChatUtil.sendNotice(sender, "New item sorter network '" + name + "' created!");

                    PixieCommandSession session = sessions.getSession(PixieCommandSession.class, sender);
                    session.setCurrentNetwork(optNetworkID.get());
                });
            });
        }

        @Command(aliases = {"use", "select"},
                usage = "<network name>", desc = "Work with an existing sorter system", min = 1, max = 1)
        public void useCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);
            String name = args.getString(0).toUpperCase();

            manager.selectNetwork(owner.getUniqueId(), name).thenAccept((optNetworkID) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optNetworkID.isEmpty()) {
                        ChatUtil.sendError(sender, "Failed to find an item sorter network by that name!");
                        return;
                    }

                    ChatUtil.sendNotice(sender, "Item sorter network '" + name + "' selected!");

                    PixieCommandSession session = sessions.getSession(PixieCommandSession.class, sender);
                    session.setCurrentNetwork(optNetworkID.get());
                });
            });
        }

        @Command(aliases = {"add"}, desc = "Add chests to the item sorter")
        @NestedCommand({AdditionCommands.class})
        public void subCmds(CommandContext args, CommandSender sender) throws CommandException {
        }
    }

    public class AdditionCommands {
        @Command(aliases = {"source"}, desc = "Add a source to the network", min = 0, max = 0)
        public void addSourceCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);

            PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);
            session.setCommandAction(PixieCommand.ADD_SOURCE);

            ChatUtil.sendNotice(sender, "Punch the chest you'd like to make a source.");
        }

        @Command(aliases = {"sink"}, desc = "Add a sink to the network", min = 0, max = 0)
        public void addSinkCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);

            PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);
            session.setCommandAction(PixieCommand.ADD_SINK);

            ChatUtil.sendNotice(sender, "Punch the chest you'd like to make a sink.");
        }
    }
}
