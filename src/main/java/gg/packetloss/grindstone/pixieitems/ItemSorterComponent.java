/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems;

import com.google.common.collect.ImmutableSet;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.pixieitems.broker.EconomyBroker;
import gg.packetloss.grindstone.pixieitems.broker.VoidBroker;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;
import gg.packetloss.grindstone.pixieitems.manager.PixieNetworkManager;
import gg.packetloss.grindstone.pixieitems.manager.ThreadedPixieNetworkManager;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static gg.packetloss.grindstone.util.EnvironmentUtil.isChest;

@ComponentInformation(friendlyName = "Item Sorter", desc = "A system of magical pixie item sorting.")
@Depend(components = {SessionComponent.class, ManagedWorldComponent.class, WalletComponent.class})
public class ItemSorterComponent extends BukkitComponent implements Listener {
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

        CommandBook.registerEvents(this);

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            PixieSinkCreationModeConverter.register(registrar);

            registrar.registerAsSubCommand("/sorter","Item sorter system management", (sorterRegistrar) -> {
                sorterRegistrar.registerAsSubCommand("network", "View, select, and manage networks", (networkRegistrar) -> {
                    networkRegistrar.register(ItemSorterNetworkCommandsRegistration.builder(), new ItemSorterNetworkCommands(sessions, manager));
                });
                sorterRegistrar.registerAsSubCommand("add", "Add containers to the current sorter network", (addRegistrar) -> {
                    addRegistrar.register(ItemSorterAddCommandsRegistration.builder(), new ItemSorterAddCommands(sessions));
                });
            });
        });

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                CommandBook.inst(),
                this::processQueuedInventories,
                TimeUtil.convertMinutesToTicks(5),
                TimeUtil.convertMinutesToTicks(5)
        );
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(
            net.milkbowl.vault.economy.Economy.class
        );
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
            Material.BARREL, Material.ENDER_CHEST
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
        Bukkit.getScheduler().runTask(CommandBook.inst(), () -> {
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

                manager.selectNetwork(networkID).thenAccept((optNetworkDetail) -> {
                    if (optNetworkDetail.isEmpty()) {
                        return;
                    }

                    PixieNetworkDetail networkDetail = optNetworkDetail.get();
                    Location origin = networkDetail.getOrigin();
                    if (!LocationUtil.isWithin2DDistance(block.getLocation(), origin, 100)) {
                        ChatUtil.sendError(player, "This block is too far away from the network creation point.");
                        return;
                    }

                    manager.addSource(networkID, block).thenAcceptAsynchronously((result) -> {
                        if (result.isNew()) {
                            ChatUtil.sendNotice(player, "Chest updated to source!");
                        } else {
                            ChatUtil.sendError(player, "Chest is already a source!");
                        }
                    });
                });
                break;
            }
            case ADD_SINK: {
                if (!shouldConsiderAsSink(block)) {
                    return;
                }

                manager.addSink(player, networkID, block, session.getSinkCreationMode()).thenAcceptAsynchronously((result) -> {
                    ChatUtil.sendNotice(player, "Container updated to sink! Accepts:");

                    ImmutableSet<String> sinkItems = result.getItemNames();
                    if (sinkItems.isEmpty()) {
                        ChatUtil.sendNotice(player, " - " + ChatColor.BLUE + "ANYTHING");
                    } else {
                        for (String sinkItem : sinkItems) {
                            ChatUtil.sendNotice(player, " - " + ItemNameCalculator.getSystemDisplayName(sinkItem));
                        }
                    }
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

        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
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
                if (optNetworkDetail.isEmpty()) {
                    return;
                }

                if (!inventory.getViewers().isEmpty()) {
                    return;
                }

                PixieNetworkDetail networkDetail = optNetworkDetail.get();

                OfflinePlayer player = GeneralPlayerUtil.findOfflinePlayer(networkDetail.getNamespace());
                TransactionBroker broker = player != null ?  new EconomyBroker(economy, player) : new VoidBroker();
                manager.sourceItems(player, broker, networkID, inventory);
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
}
