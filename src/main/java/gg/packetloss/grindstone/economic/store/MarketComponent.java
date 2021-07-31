/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.store.command.*;
import gg.packetloss.grindstone.economic.store.mysql.MySQLItemStoreDatabase;
import gg.packetloss.grindstone.economic.store.mysql.MySQLMarketTransactionDatabase;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.events.economy.MarketPurchaseEvent;
import gg.packetloss.grindstone.events.economy.MarketSellEvent;
import gg.packetloss.grindstone.invgui.InventoryGUIComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.task.promise.FailableTaskFuture;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;
import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemNames;

@ComponentInformation(friendlyName = "Market", desc = "Buy and sell goods.")
@Depend(plugins = {"WorldGuard"}, components = {AdminComponent.class, DataBaseComponent.class, WalletComponent.class})
public class MarketComponent extends BukkitComponent {
    public static final int LOWER_MARKET_LOSS_THRESHOLD = 100000;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private InventoryGUIComponent invGUI;
    @InjectComponent
    private WalletComponent wallet;

    private static ItemStoreDatabase itemDatabase;
    private static MarketTransactionDatabase transactionDatabase;

    private ProtectedRegion region = null;

    public void simulateMarket(int restockingRounds) {
        itemDatabase.updatePrices(restockingRounds);

        ChatUtil.sendNotice(server.getOnlinePlayers(), ChatColor.GOLD + "The market has been updated!");
    }

    public void simulateMarket() {
        simulateMarket(1);
    }

    @Override
    public void enable() {
        itemDatabase = new MySQLItemStoreDatabase();
        itemDatabase.load();

        transactionDatabase = new MySQLMarketTransactionDatabase();
        transactionDatabase.load();

        CommandBook.registerEvents(new MarketTransactionLogger(transactionDatabase));

        // Register user facing commands
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            MarketItemConverter.register(registrar, this);
            MarketItemSetConverter.register(registrar, this);

            registrar.registerAsSubCommand("market", Set.of("mk"), "Admin Market", (marketRegistrar) -> {
                marketRegistrar.register(MarketCommandsRegistration.builder(), new MarketCommands(this, invGUI, wallet));

                marketRegistrar.registerAsSubCommand("admin", "Admin Market Control Commands", (adminMarketRegistrar) -> {
                    adminMarketRegistrar.register(MarketAdminCommandsRegistration.builder(), new MarketAdminCommands(this, wallet));
                });
            });
        });

        // Get the region
        region = WorldGuardBridge.getManagerFor(Bukkit.getWorld("City")).getRegion("vineam-district-bank");

        // Calculate delay
        int nextEventHour = TimeUtil.getNextHour((hour) -> hour % 2 == 0);
        long nextRunDelay = TimeUtil.getTicksTill(nextEventHour);

        // Schedule an update task for every two hours
        server.getScheduler().runTaskTimerAsynchronously(
          inst, (Runnable) this::simulateMarket, nextRunDelay, TimeUtil.convertHoursToTicks(2)
        );
    }

    @Override
    public void reload() {
        itemDatabase.load();
    }

    public static final String NOT_AVAILIBLE = "No item by that name is currently available!";

    public TaskFuture<List<ItemTransaction>> getTransactions(String itemName, UUID playerID) {
        return TaskFuture.asyncTask(() -> {
            return transactionDatabase.getTransactions(itemName, playerID);
        });
    }

    public TaskFuture<Void> scaleMarket(double factor) {
        return TaskFuture.asyncTask(() -> {
            List<MarketItemInfo> items = itemDatabase.getItemList();
            for (MarketItemInfo item : items) {
                itemDatabase.addItem(item.getName(), item.getPrice() * factor, !item.isBuyable(), !item.isSellable());
            }
            itemDatabase.save();

            return null;
        });
    }

    public TaskFuture<MarketItemInfo> addItem(String itemName, double price, boolean disableBuy, boolean disableSell) {
        return TaskFuture.asyncTask(() -> {
            MarketItemInfo oldItem = itemDatabase.getItem(itemName);

            itemDatabase.addItem(itemName, price, disableBuy, disableSell);
            itemDatabase.save();

            return oldItem;
        });
    }

    public TaskFuture<Void> removeItem(String itemName) {
        return TaskFuture.asyncTask(() -> {
            itemDatabase.removeItem(itemName);
            itemDatabase.save();

            return null;
        });
    }

    // FIXME: This needs pulled out
    public static ItemStack getBaseStack(String name) throws CommandException {
        try {
            if (name.startsWith("grindstone:")) {
                name = name.replaceFirst("grindstone:", "");
                CustomItems item = CustomItems.valueOf(name.toUpperCase());
                return CustomItemCenter.build(item);
            }


            return new ItemStack(Objects.requireNonNull(Material.matchMaterial(name)), 1);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new CommandException("Please report this error, " + name + " could not be found.");
        }
    }

    private ItemStack[] getItem(MarketTransactionLine transactionLine) throws CommandException {
        List<ItemStack> itemStacks = new ArrayList<>();

        ItemStack stack = getBaseStack(transactionLine.getItem().getName());
        for (int i = transactionLine.getAmount(); i > 0;) {
            ItemStack cloned = stack.clone();
            cloned.setAmount(Math.min(stack.getMaxStackSize(), i));
            i -= cloned.getAmount();
            itemStacks.add(cloned);
        }

        return itemStacks.toArray(new ItemStack[0]);
    }

    public static MarketItemLookupInstance getLookupInstance(Set<String> itemNames) {
        return new MarketItemLookupInstance(itemDatabase.getItems(itemNames));
    }

    public static MarketItemLookupInstance getLookupInstanceFromStacksImmediately(Collection<ItemStack> stacks) {
        return getLookupInstance(computeItemNames(stacks));
    }

    public static TaskFuture<MarketItemLookupInstance> getLookupInstanceFromStacks(Collection<ItemStack> stacks) {
        Set<String> names = computeItemNames(stacks);
        return TaskFuture.asyncTask(() -> {
            return getLookupInstance(names);
        });
    }

    private BigDecimal calculateTotalPriceAndVerifyStock(List<MarketTransactionLine> transactionLines) throws CommandException {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (MarketTransactionLine transactionLine : transactionLines) {
            MarketItem item = transactionLine.getItem();
            int amount = transactionLine.getAmount();

            if (transactionLine.getAmount() > item.getStock()) {
                throw new CommandException("You requested " +
                    ChatUtil.WHOLE_NUMBER_FORMATTER.format(amount) + " however, only " +
                    ChatUtil.WHOLE_NUMBER_FORMATTER.format(item.getStock()) + " are in stock.");
            }

            totalPrice = totalPrice.add(new BigDecimal(item.getPrice() * amount));
        }

        return totalPrice;
    }

    // FIXME: This method does too much, shouldn't have to deal with itemstacks here
    public FailableTaskFuture<BigDecimal, String> buyItems(Player player, List<MarketTransactionLine> transactionLines) throws CommandException {
        BigDecimal totalPrice = calculateTotalPriceAndVerifyStock(transactionLines);

        return wallet.removeFromBalance(player, totalPrice).thenApplyFailableAsynchronously(
            TaskResult::fromCondition,
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        ).thenAcceptAsynchronously(
            (ignored) -> {
                // Update market stocks.
                itemDatabase.adjustStocksForBuy(transactionLines);
            },
            (ignored) -> {
                ChatUtil.sendError(player, "You do not have enough money to purchase that item(s).");
            }
        ).thenApplyFailable(
            (ignored) -> {
                CommandBook.callEvent(new MarketPurchaseEvent(player, transactionLines, totalPrice));

                // Get the items and add them to the inventory
                try {
                    for (MarketTransactionLine transactionLine : transactionLines) {
                        ItemStack[] itemStacks = getItem(transactionLine);
                        for (ItemStack itemStack : itemStacks) {
                            if (player.getInventory().firstEmpty() == -1) {
                                player.getWorld().dropItem(player.getLocation(), itemStack);
                                continue;
                            }
                            player.getInventory().addItem(itemStack);
                        }
                    }
                    return TaskResult.of(totalPrice);
                } catch (CommandException ex) {
                    return TaskResult.failed(ex.getMessage());
                }
            }
        );
    }

    public FailableTaskFuture<BigDecimal, Void> sellItems(Player player, List<MarketTransactionLine> transactionLines, BigDecimal payment) {
        // Update market stocks.
        itemDatabase.adjustStocksForSale(transactionLines);

        CommandBook.callEvent(new MarketSellEvent(player, transactionLines, payment));

        // Deposit the money
        return wallet.addToBalance(player, payment);
    }

    private List<MarketItem> getItemListFor(String filter, boolean canSeeDisabled) {
        return itemDatabase.getItemList(filter, canSeeDisabled)
            .stream()
            .map(MarketItem::new)
            .collect(Collectors.toList());
    }

    public List<MarketItem> getItemListFor(CommandSender sender, String filter) {
        boolean canSeeDisabled = sender.hasPermission("aurora.admin.adminstore.disabled");
        return getItemListFor(filter, canSeeDisabled);
    }

    public TaskFuture<List<MarketItem>> asyncGetItemListFor(CommandSender sender, String filter) {
        boolean canSeeDisabled = sender.hasPermission("aurora.admin.adminstore.disabled");
        return TaskFuture.asyncTask(() -> {
            return getItemListFor(filter, canSeeDisabled);
        });
    }

    public void checkPlayer(CommandSender sender) throws CommandException {
        Player player = PlayerUtil.checkPlayer(sender);

        if (adminComponent.isAdmin(player)) {
            throw new CommandException("You cannot use this command while in admin mode.");
        }

        checkInArea(player);
    }

    public void checkInArea(Player player) throws CommandException {
        if (!isInArea(player.getLocation())) {
            throw new CommandException("You call out, but no one hears your offer.");
        }
    }

    public boolean isInArea(Location location) {
        return location.getWorld().getName().equals("City") && region != null && region.contains(toBlockVec3(location));
    }
}
