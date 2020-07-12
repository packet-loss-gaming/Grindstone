/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.PaginatedResult;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.store.command.MarketCommands;
import gg.packetloss.grindstone.economic.store.command.MarketCommandsRegistration;
import gg.packetloss.grindstone.economic.store.command.MarketItemConverter;
import gg.packetloss.grindstone.economic.store.command.MarketItemSetConverter;
import gg.packetloss.grindstone.economic.store.mysql.MySQLItemStoreDatabase;
import gg.packetloss.grindstone.economic.store.mysql.MySQLMarketTransactionDatabase;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import gg.packetloss.grindstone.events.economy.MarketPurchaseEvent;
import gg.packetloss.grindstone.events.economy.MarketSellEvent;
import gg.packetloss.grindstone.invgui.InventoryGUIComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;
import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemNames;
import static gg.packetloss.grindstone.util.item.ItemNameCalculator.matchItem;

@ComponentInformation(friendlyName = "Market", desc = "Buy and sell goods.")
@Depend(plugins = {"WorldGuard"}, components = {AdminComponent.class, DataBaseComponent.class, SessionComponent.class})
public class MarketComponent extends BukkitComponent {
    public static final int LOWER_MARKET_LOSS_THRESHOLD = 100000;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private InventoryGUIComponent invGUI;

    private static ItemStoreDatabase itemDatabase;
    private static MarketTransactionDatabase transactionDatabase;

    private ProtectedRegion region = null;
    private Economy econ;

    public void simulateMarket(int restockingRounds) {
        itemDatabase.updatePrices(restockingRounds);

        ChatUtil.sendNotice(server.getOnlinePlayers(), ChatColor.GOLD + "The market has been updated!");
    }

    public void simulateMarket() {
        simulateMarket(1);
    }

    @Override
    public void enable() {
        // Setup economy
        setupEconomy();

        itemDatabase = new MySQLItemStoreDatabase();
        itemDatabase.load();

        transactionDatabase = new MySQLMarketTransactionDatabase();
        transactionDatabase.load();

        CommandBook.registerEvents(new MarketTransactionLogger(transactionDatabase));

        // Register user facing commands
        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        MarketItemConverter.register(registrar, this);
        MarketItemSetConverter.register(registrar, this);
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            registrar.registerAsSubCommand("market", Set.of("mk"), "Admin Market", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, MarketCommandsRegistration.builder(), new MarketCommands(this, invGUI, econ));
            });
        });

        // Register admin commands
        registerCommands(Commands.class);

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

    // FIXME: These need rewritten
    public class Commands {
        @Command(aliases = {"marketadmin"}, desc = "Admin Store commands")
        @NestedCommand({AdminStoreCommands.class})
        public void storeCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class AdminStoreCommands {

        @Command(aliases = {"log"},
                usage = "[-i item] [-u user] [-p page]", desc = "Item database logs",
                flags = "i:u:p:s", min = 0, max = 0)
        @CommandPermissions("aurora.admin.adminstore.log")
        public void logCmd(CommandContext args, CommandSender sender) throws CommandException {
            String item = args.getFlag('i', null);
            if (item != null) {
                Optional<String> optItemName = matchItem(item);
                if (optItemName.isPresent()) {
                    item = optItemName.get();
                } else {
                    throw new CommandException("No item by that name was found.");
                }
            }
            String player = args.getFlag('u', null);

            List<ItemTransaction> transactions = transactionDatabase.getTransactions(item, player);
            new PaginatedResult<ItemTransaction>(ChatColor.GOLD + "Market Transactions") {
                @Override
                public String format(ItemTransaction trans) {
                    String message = ChatColor.YELLOW + trans.getPlayer() + ' ';
                    if (trans.getAmount() > 0) {
                        message += ChatColor.RED + "bought";
                    } else {
                        message += ChatColor.DARK_GREEN + "sold";
                    }
                    message += " " + ChatColor.YELLOW + Math.abs(trans.getAmount())
                            + ChatColor.BLUE + " " + trans.getItem().toUpperCase();
                    return message;
                }
            }.display(sender, transactions, args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"scale"},
                usage = "<amount>", desc = "Scale the item database",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.admin.adminstore.scale")
        public void scaleCmd(CommandContext args, CommandSender sender) throws CommandException {
            double factor = args.getDouble(0);

            if (factor == 0) {
                throw new CommandException("Cannot scale by 0.");
            }

            server.getScheduler().runTaskAsynchronously(inst, () -> {
                List<MarketItemInfo> items = itemDatabase.getItemList();
                for (MarketItemInfo item : items) {
                    itemDatabase.addItem(sender.getName(), item.getName(),
                      item.getPrice() * factor, !item.isBuyable(), !item.isSellable());
                }
                itemDatabase.save();

                ChatUtil.sendNotice(sender, "Market Scaled by: " + factor + ".");
            });
        }

        @Command(aliases = {"add"},
                usage = "[-p price] <item name>", desc = "Add an item to the database",
                flags = "bsp:", min = 1)
        @CommandPermissions("aurora.admin.adminstore.add")
        public void addCmd(CommandContext args, CommandSender sender) throws CommandException {
            Optional<String> optItemName = matchItem(args.getJoinedStrings(0));
            if (optItemName.isEmpty()) {
                throw new CommandException("No item by that name was found.");
            }
            String itemName = optItemName.get();

            boolean disableBuy = args.hasFlag('b');
            boolean disableSell = args.hasFlag('s');

            double price = Math.max(.01, args.getFlagDouble('p', .1));

            // Database operations
            MarketItemInfo oldItem = itemDatabase.getItem(itemName);
            itemDatabase.addItem(sender.getName(), itemName, price, disableBuy, disableSell);
            itemDatabase.save();

            // Notification
            String noticeString = oldItem == null ? " added with a price of " : " is now ";
            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), " ");
            ChatUtil.sendNotice(sender, ChatColor.BLUE + itemName.toUpperCase() + ChatColor.YELLOW + noticeString + priceString + "!");
            if (disableBuy) {
                ChatUtil.sendNotice(sender, " - It cannot be purchased.");
            }
            if (disableSell) {
                ChatUtil.sendNotice(sender, " - It cannot be sold.");
            }
        }

        @Command(aliases = {"remove"},
          usage = "<item name>", desc = "Value an item",
          flags = "", min = 1)
        @CommandPermissions("aurora.admin.adminstore.remove")
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Optional<String> optItemName = matchItem(args.getJoinedStrings(0));
            if (optItemName.isEmpty()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            String itemName = optItemName.get();
            if (itemDatabase.getItem(itemName) == null) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            itemDatabase.removeItem(sender.getName(), itemName);
            itemDatabase.save();
            ChatUtil.sendNotice(sender, ChatColor.BLUE + itemName.toUpperCase() + ChatColor.YELLOW + " has been removed from the database!");
        }

        @Command(aliases = {"simulate"}, desc = "Simulate market activity",
          usage = "[rounds]", flags = "", min = 0, max = 1)
        @CommandPermissions("aurora.admin.adminstore.simulate")
        public void simulateCmd(CommandContext args, CommandSender sender) throws CommandException {
            server.getScheduler().runTaskAsynchronously(inst, () -> {
                simulateMarket(Math.max(1, args.getInteger(0, 1)));
            });
        }
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

    public static CompletableFuture<MarketItemLookupInstance> getLookupInstanceFromStacks(Collection<ItemStack> stacks) {
        CompletableFuture<MarketItemLookupInstance> future = new CompletableFuture<>();

        Set<String> names = computeItemNames(stacks);

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            future.complete(getLookupInstance(names));
        });

        return future;
    }

    // FIXME: This method does too much, shouldn't have to deal with itemstacks here
    public double buyItems(Player player, List<MarketTransactionLine> transactionLines) throws CommandException {
        double totalPrice = 0;

        for (MarketTransactionLine transactionLine : transactionLines) {
            MarketItem item = transactionLine.getItem();
            int amount = transactionLine.getAmount();

            if (transactionLine.getAmount() > item.getStock()) {
                throw new CommandException("You requested " +
                        ChatUtil.WHOLE_NUMBER_FORMATTER.format(amount) + " however, only " +
                        ChatUtil.WHOLE_NUMBER_FORMATTER.format(item.getStock()) + " are in stock.");
            }

            totalPrice += item.getPrice() * amount;
        }

        // Check funds
        if (!econ.has(player, totalPrice)) {
            throw new CommandException("You do not have enough money to purchase that item(s).");
        }

        // Charge the money
        econ.withdrawPlayer(player, totalPrice);

        // Update market stocks.
        itemDatabase.adjustStocksForBuy(transactionLines);

        CommandBook.callEvent(new MarketPurchaseEvent(player, transactionLines, totalPrice));

        // Get the items and add them to the inventory
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

        return totalPrice;
    }

    public void sellItems(Player player, List<MarketTransactionLine> transactionLines, double payment) {
        // Update market stocks.
        itemDatabase.adjustStocksForSale(transactionLines);

        CommandBook.callEvent(new MarketSellEvent(player, transactionLines, payment));

        // Deposit the money
        econ.depositPlayer(player, payment);
    }

    public List<MarketItem> getItemListFor(CommandSender sender, String filter) {
        return itemDatabase.getItemList(filter, sender.hasPermission("aurora.admin.adminstore.disabled"))
                .stream()
                .map(MarketItem::new)
                .collect(Collectors.toList());
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

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        }

        return (econ != null);
    }
}
