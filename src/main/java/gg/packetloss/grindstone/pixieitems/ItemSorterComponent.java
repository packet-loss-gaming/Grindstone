package gg.packetloss.grindstone.pixieitems;

import com.google.common.collect.ImmutableSet;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.pixieitems.broker.EconomyBroker;
import gg.packetloss.grindstone.pixieitems.broker.VoidBroker;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;
import gg.packetloss.grindstone.pixieitems.manager.PixieNetworkManager;
import gg.packetloss.grindstone.pixieitems.manager.ThreadedPixieNetworkManager;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.EnvironmentUtil.isChest;

@ComponentInformation(friendlyName = "Item Sorter", desc = "A system of magical pixie item sorting.")
@Depend(components = {SessionComponent.class, ManagedWorldComponent.class})
public class ItemSorterComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private ManagedWorldComponent managedWorld;

    private Economy economy = null;

    private PixieNetworkManager manager = new ThreadedPixieNetworkManager();

    private Lock queuedInventoriesLock = new ReentrantLock();
    private Set<Inventory> queuedInventories = new HashSet<>();

    @Override
    public void enable() {
        setupEconomy();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        registerCommands(Commands.class);

        server.getScheduler().runTaskTimerAsynchronously(
                inst,
                this::processQueuedInventories,
                TimeUtil.convertMinutesToTicks(5),
                TimeUtil.convertMinutesToTicks(5)
        );
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    private boolean shouldConsiderCommon(Block block) {
        if (!managedWorld.is(ManagedWorldIsQuery.ANY_BUIDABLE, block.getWorld())) {
            return false;
        }

        return true;
    }

    private boolean shouldConsiderAsSource(Block block) {
        if (!isChest(block)) {
            return false;
        }

        return shouldConsiderCommon(block);
    }

    private static final Set<Material> SINK_CONTAINERS = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.DISPENSER, Material.DROPPER, Material.SHULKER_BOX,
            Material.BARREL
    );

    private boolean shouldConsiderAsSink(Block block) {
        if (!SINK_CONTAINERS.contains(block.getType())) {
            return false;
        }

        return shouldConsiderCommon(block);
    }

    private boolean shouldConsiderAsAny(Block block) {
        return shouldConsiderAsSource(block) || shouldConsiderAsSink(block);
    }

    private Set<Inventory> swapInventories() {
        try {
            queuedInventoriesLock.lock();

            // If there's nothing there, return an empty list
            if (queuedInventories.isEmpty()) {
                return Set.of();
            }

            Set<Inventory> oldInventories = queuedInventories;
            // Reset the queue to a slightly smaller list to prevent frequent allocations
            queuedInventories = new HashSet<>((int) (oldInventories.size() * .9) + 15);
            return oldInventories;
        } finally {
            queuedInventoriesLock.unlock();
        }
    }

    public void processQueuedInventories() {
        Set<Inventory> oldInventories = swapInventories();
        server.getScheduler().runTask(inst, () -> {
            oldInventories.forEach(this::processInventory);
        });
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

        // We should always have a network ID if a command action is set.
        int networkID = session.getCurrentNetwork().get().getID();
        PixieCommand command = session.getCurrentCommand();
        switch (command) {
            case ADD_SOURCE: {
                if (!shouldConsiderAsSource(block)) {
                    return;
                }

                manager.addSource(networkID, block).thenAccept((result) -> {
                    if (result.isNew()) {
                        ChatUtil.sendNotice(player, "Chest updated to source!");
                    } else {
                        ChatUtil.sendError(player, "Chest is already a source!");
                    }
                }).exceptionally((ex) -> {
                    ex.printStackTrace();
                    ChatUtil.sendError(player, "An error occurred while attempting to create this source.");
                    return null;
                });
                break;
            }
            case ADD_SINK: {
                if (!shouldConsiderAsSink(block)) {
                    return;
                }

                manager.addSink(networkID, block, session.getTargetSinkVariant()).thenAccept((result) -> {
                    ChatUtil.sendNotice(player, "Container updated to sink! Accepts:");

                    ImmutableSet<String> sinkItems = result.getItemNames();
                    if (sinkItems.isEmpty()) {
                        ChatUtil.sendNotice(player, " - " + ChatColor.BLUE + "ANYTHING");
                    } else {
                        for (String sinkItem : sinkItems) {
                            ChatUtil.sendNotice(player, " - " + ChatColor.BLUE + sinkItem.toUpperCase());
                        }
                    }
                }).exceptionally((ex) -> {
                    ex.printStackTrace();
                    ChatUtil.sendError(player, "An error occurred while attempting to create this sink.");
                    return null;
                });
                break;
            }
        }

        session.performedAction();
    }

    private boolean shouldConsiderChestExpansion(Block block) {
        if (!isChest(block)) {
            return false;
        }

        return shouldConsiderCommon(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (!shouldConsiderChestExpansion(block)) {
            return;
        }

        manager.maybeExpandChest(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!shouldConsiderAsAny(block)) {
            return;
        }

        manager.removeContainer(block.getLocation());
    }

    private void processInventory(Inventory inventory) {
        if (inventory.getType() != InventoryType.CHEST) {
            return;
        }

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

                optNetworkID = manager.getNetworkFromSourceContainers(leftChest.getBlock(), rightChest.getBlock());
            } else if (holder instanceof Chest) { // Mule inventories count as chests for some reason
                Chest chest = (Chest) holder;
                optNetworkID = manager.getNetworkFromSourceContainers(chest.getBlock());
            } else {
                return;
            }

            if (optNetworkID.isEmpty()) {
                return;
            }

            int networkID = optNetworkID.get();
            manager.selectNetwork(networkID).thenAccept((optNetworkDetail) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optNetworkDetail.isEmpty()) {
                        return;
                    }

                    if (!inventory.getViewers().isEmpty()) {
                        return;
                    }

                    PixieNetworkDetail networkDetail = optNetworkDetail.get();

                    OfflinePlayer player = Bukkit.getOfflinePlayer(networkDetail.getNamespace());
                    TransactionBroker broker = player != null ?  new EconomyBroker(economy, player) : new VoidBroker();

                    manager.sourceItems(broker, networkID, inventory);
                });
            });
        }, 1);
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        processInventory(inventory);
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        try {
            queuedInventoriesLock.lock();

            queuedInventories.add(event.getDestination());
        } finally {
            queuedInventoriesLock.unlock();
        }
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
        @Command(aliases = {"network", "networks"}, desc = "View, select, and manage networks")
        @NestedCommand({NetworkCommands.class})
        public void networksSubCmds(CommandContext args, CommandSender sender) throws CommandException {
        }

        @Command(aliases = {"add"}, desc = "Add chests to the current sorter network")
        @NestedCommand({AdditionCommands.class})
        public void additionSubCmds(CommandContext args, CommandSender sender) throws CommandException {
        }
    }

    public class NetworkCommands {
        @Command(aliases = {"create", "add"},
                 usage = "<network name>", desc = "Create a new sorter system", min = 1, max = 1)
        public void createCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);
            String name = args.getString(0).toUpperCase();

            manager.createNetwork(owner.getUniqueId(), name).thenAccept((optNetworkDetail) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optNetworkDetail.isEmpty()) {
                        ChatUtil.sendError(sender, "Failed to create network!");
                        return;
                    }

                    ChatUtil.sendNotice(sender, "New item sorter network '" + name + "' created!");

                    PixieCommandSession session = sessions.getSession(PixieCommandSession.class, sender);
                    session.setCurrentNetwork(optNetworkDetail.get());
                });
            });
        }

        @Command(aliases = {"use", "select"},
                 usage = "<network name>", desc = "Work with an existing sorter system", min = 1, max = 1)
        public void useCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);
            String name = args.getString(0).toUpperCase();

            manager.selectNetwork(owner.getUniqueId(), name).thenAccept((optNetworkDetail) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optNetworkDetail.isEmpty()) {
                        ChatUtil.sendError(sender, "Failed to find an item sorter network by that name!");
                        return;
                    }

                    ChatUtil.sendNotice(sender, "Item sorter network '" + name + "' selected!");

                    PixieCommandSession session = sessions.getSession(PixieCommandSession.class, sender);
                    session.setCurrentNetwork(optNetworkDetail.get());
                });
            });
        }

        @Command(aliases = {"list"}, desc = "List sorter networks",
                 usage = "[-p <page>]", min = 0, max = 0)
        public void listNetworksCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);

            manager.selectNetworks(owner.getUniqueId()).thenAccept((networks) -> {
                Collections.sort(networks);
                server.getScheduler().runTask(inst, () -> {
                    new TextComponentChatPaginator<PixieNetworkDetail>(ChatColor.GOLD, "Networks") {
                        @Override
                        public Optional<String> getPagerCommand(int page) {
                            return Optional.of("//sorter networks list -p " + page);
                        }

                        @Override
                        public Text format(PixieNetworkDetail network) {
                            return Text.of(
                                    ChatColor.BLUE,
                                    network.getName(),
                                    TextAction.Hover.showText(Text.of("Use ", network.getName()," network")),
                                    TextAction.Click.runCommand("//sorter networks use " + network.getName())
                            );
                        }
                    }.display(sender, networks, args.getFlagInteger('p', 1));
                });
            });
        }
    }

    public class AdditionCommands {
        @Command(aliases = {"source"}, desc = "Add a source to the network", min = 0, max = 0)
        public void addSourceCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);

            PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);
            session.commandToAddSource();

            ChatUtil.sendNotice(sender, "Punch the chest you'd like to make a source.");
        }

        @Command(aliases = {"sink"}, desc = "Add a sink to the network", usage = "[mode]", min = 0, max = 1)
        public void addSinkCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player owner = PlayerUtil.checkPlayer(sender);

            PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);

            String variant = args.getString(0, "overwrite").toUpperCase();
            try {
                session.commandToAddSink(PixieSinkVariant.valueOf(variant));
            } catch (IllegalArgumentException ex) {
                throw new CommandException("Valid modes are: overwrite, add, void");
            }

            ChatUtil.sendNotice(sender, "Punch the container you'd like to make a sink.");
        }
    }
}
