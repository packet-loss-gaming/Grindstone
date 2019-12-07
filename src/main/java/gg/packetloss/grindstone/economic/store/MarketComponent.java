/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.bukkittext.TextBuilder;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.store.mysql.MySQLItemStoreDatabase;
import gg.packetloss.grindstone.economic.store.mysql.MySQLMarketTransactionDatabase;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.item.InventoryUtil.InventoryView;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.StringUtil.toTitleCase;
import static gg.packetloss.grindstone.util.item.ItemNameCalculator.*;

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
        // FIXME: Work around for database load order issue.
        server.getScheduler().runTaskLater(inst, () -> {
            itemDatabase = new MySQLItemStoreDatabase();
            itemDatabase.load();
            transactionDatabase = new MySQLMarketTransactionDatabase();
            transactionDatabase.load();

            // Setup external systems
            setupEconomy();
            // Register commands
            registerCommands(Commands.class);

            // Get the region
            server.getScheduler().runTaskLater(inst, () -> {
                try {
                    region = getWorldGuard().getGlobalRegionManager().get(Bukkit.getWorld("City")).getRegion("vineam-district-bank");
                } catch (UnknownPluginException e) {
                    e.printStackTrace();
                }
            }, 1);

            // Calculate delay
            int nextEventHour = TimeUtil.getNextHour((hour) -> hour % 2 == 0);
            long nextRunDelay = TimeUtil.getTicksTill(nextEventHour);

            // Schedule an update task for every two hours
            server.getScheduler().runTaskTimerAsynchronously(
              inst, this::simulateMarket, nextRunDelay, TimeUtil.convertHoursToTicks(2)
            );
        }, 1);
    }

    @Override
    public void reload() {
        itemDatabase.load();
    }

    private final String NOT_AVAILIBLE = "No item by that name is currently available!";

    public class Commands {

        @Command(aliases = {"market", "mk", "ge"}, desc = "Admin Store commands")
        @NestedCommand({StoreCommands.class})
        public void storeCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class StoreCommands {

        @Command(aliases = {"buy", "b"},
                usage = "[-a amount] <item name>", desc = "Buy an item",
                flags = "a:", min = 1)
        public void buyCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerName = checkPlayer(sender);
            Player player = (Player) sender;

            // Calculate the item names
            String nameWithMacros = args.getJoinedStrings(0);
            List<String> expandedNames = ItemNameCalculator.expandNameMacros(nameWithMacros);

            // Calculate the quantities
            int amt = 1;
            if (args.hasFlag('a')) {
                amt = Math.max(1, args.getFlagInteger('a'));
            }

            // Start counting the total
            double totalPrice = 0;

            // Reserve space for resolved items
            List<MarketItemInfo> resolvedItems = new ArrayList<>(expandedNames.size());
            for (String expandedName : expandedNames) {
                Optional<String> optItemName = matchItemFromNameOrId(expandedName);
                if (optItemName.isEmpty()) {
                    throw new CommandException(NOT_AVAILIBLE);
                }

                MarketItemInfo marketItemInfo = itemDatabase.getItem(optItemName.get());
                if (marketItemInfo == null || !marketItemInfo.isBuyable()) {
                    throw new CommandException(NOT_AVAILIBLE);
                }

                if (amt > marketItemInfo.getStock()) {
                    throw new CommandException("You requested " +
                            ChatUtil.WHOLE_NUMBER_FORMATTER.format(amt) + " however, only " +
                            ChatUtil.WHOLE_NUMBER_FORMATTER.format(marketItemInfo.getStock()) + " are in stock.");
                }

                totalPrice += marketItemInfo.getPrice() * amt;

                resolvedItems.add(marketItemInfo);
            }

            // Check funds
            if (!econ.has(player, totalPrice)) {
                throw new CommandException("You do not have enough money to purchase that item(s).");
            }

            // Charge the money and send the sender some feedback
            econ.withdrawPlayer(player, totalPrice);

            // Deposit into the lottery account
            double lottery = totalPrice * .03;
            econ.bankDeposit("Lottery", lottery);

            // Reserve space for stock adjustments
            HashMap<String, Integer> adjustments = new HashMap<>();

            // Get the items and add them to the inventory
            for (MarketItemInfo marketItemInfo : resolvedItems) {
                String itemName = marketItemInfo.getName();

                ItemStack[] itemStacks = getItem(itemName, amt);
                for (ItemStack itemStack : itemStacks) {
                    if (player.getInventory().firstEmpty() == -1) {
                        player.getWorld().dropItem(player.getLocation(), itemStack);
                        continue;
                    }
                    player.getInventory().addItem(itemStack);
                }

                adjustments.put(itemName, -amt);
                transactionDatabase.logTransaction(playerName, itemName, amt);
            }

            // Update market stocks.
            itemDatabase.adjustStocks(adjustments);
            transactionDatabase.save();

            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(totalPrice), "");
            ChatUtil.sendNotice(sender, "Item(s) purchased for " + priceString + "!");

            // Market Command Help
            StoreSession sess = sessions.getSession(StoreSession.class, player);
            if (amt == 1 && sess.recentPurch() && sess.getLastPurch().equals(nameWithMacros)) {
                ChatUtil.sendNotice(sender, "Did you know you can specify the amount of items to buy?");
                ChatUtil.sendNotice(sender, "/market buy -a <amount> " + nameWithMacros.toUpperCase());
            }
            sess.setLastPurch(nameWithMacros);
        }

        @Command(aliases = {"sell"},
                usage = "", desc = "Sell an item",
                flags = "haus", min = 0, max = 0)
        public void sellCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerName = checkPlayer(sender);
            final Player player = (Player) sender;

            ItemStack[] itemStacks = player.getInventory().getContents();

            ItemStack filter = null;

            boolean singleItem = false;
            int min, max;

            final int hotbarLength = 9;
            final int storageLength = 27;
            if (args.hasFlag('a')) {
                min = 0;
                max = hotbarLength + storageLength;
            } else if (args.hasFlag('h')) {
                min = 0;
                max = hotbarLength;
            } else if (args.hasFlag('s')) {
                min = hotbarLength;
                max = min + storageLength;
            } else {
                min = player.getInventory().getHeldItemSlot();
                max = min + 1;

                singleItem = true;
            }

            if (!singleItem && !args.hasFlag('u')) {
                filter = player.getItemInHand();
            }

            InventoryView view = new InventoryView(min, max, filter);

            Set<String> itemNamesInFilter = new HashSet<>();

            view.operateOnInventory(itemStacks, (item) -> {
                computeItemName(item).ifPresent(itemNamesInFilter::add);
                return item;
            });

            // FIXME: This is a rat's nest.
            server.getScheduler().runTaskAsynchronously(inst, () -> {
                MarketItemLookupInstance lookupInstance = getLookupInstance(itemNamesInFilter);
                server.getScheduler().runTask(inst, () -> {
                    double[] payment = {0}; // hack to update payment from inside lambda
                    HashMap<String, Integer> transactions = new HashMap<>();

                    // Calculate the payment and transactions using the new inventory.
                    ItemStack[] newInventory = player.getInventory().getContents();
                    view.operateOnInventory(newInventory, (item) -> {
                        Optional<MarketItem> optMarketItem = lookupInstance.getItemDetails(item);
                        if (optMarketItem.isEmpty()) {
                            return item;
                        }

                        Optional<Double> optSellPrice = optMarketItem.get().getSellPriceForStack(item);
                        if (optSellPrice.isEmpty()) {
                            return item;
                        }

                        payment[0] += optSellPrice.get();

                        int amt = item.getAmount();
                        transactions.merge(optMarketItem.get().getName(), amt, Integer::sum);

                        return null;
                    });

                    if (transactions.isEmpty()) {
                        ChatUtil.sendError(player, "No sellable items found" + (view.hasFilter() ? " that matched the filter" : "") + "!");
                        return;
                    }

                    server.getScheduler().runTaskAsynchronously(inst, () -> {
                        // Update market stocks.
                        itemDatabase.adjustStocks(transactions);

                        for (Map.Entry<String, Integer> entry : transactions.entrySet()) {
                            // Invert quantity this is a sale, and the transactions are from a player perspective.
                            transactionDatabase.logTransaction(playerName, entry.getKey(), -entry.getValue());
                        }
                        transactionDatabase.save();
                    });

                    econ.depositPlayer(player, payment[0]);

                    player.getInventory().setContents(newInventory);

                    String paymentString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(payment[0]), "");
                    ChatUtil.sendNotice(player, "Item(s) sold for: " + paymentString + "!");

                    // Market Command Help
                    StoreSession sess = sessions.getSession(StoreSession.class, player);
                    if (view.isSingleItem() && sess.recentSale() && !sess.recentNotice()) {
                        ChatUtil.sendNotice(sender, "Did you know you can sell more than one stack at a time?");
                        ChatUtil.sendNotice(sender, "To sell all of what you're holding:");
                        ChatUtil.sendNotice(sender, "/market sell -a");
                        ChatUtil.sendNotice(sender, "To sell everything in your inventory:");
                        ChatUtil.sendNotice(sender, "/market sell -au");
                        sess.updateNotice();
                    }
                    sess.updateSale();
                });
            });
        }

        private Text formatPriceForList(double price) {
            String result = "";
            result += ChatColor.WHITE;
            if (price >= 10000) {
                result += "~" + ChatUtil.WHOLE_NUMBER_FORMATTER.format(price / 1000) + "k";
            } else if (price >= 1000) {
                result += "~" + ChatUtil.ONE_DECIMAL_FORMATTER.format(price / 1000) + "k";
            } else {
                result += ChatUtil.TWO_DECIMAL_FORMATTER.format(price);
            }
            return Text.of(result);
        }

        @Command(aliases = {"list", "l", "search", "s"},
                usage = "[-p page] [filter...]", desc = "Get a list of items and their prices",
                flags = "p:", min = 0
        )
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {

            String filterString = args.argsLength() > 0 ? args.getJoinedStrings(0) : null;
            List<MarketItemInfo> marketItemInfoCollection = itemDatabase.getItemList(filterString,
                    inst.hasPermission(sender, "aurora.admin.adminstore.disabled"));
            Collections.sort(marketItemInfoCollection);

            new TextComponentChatPaginator<MarketItemInfo>(ChatColor.GOLD, "Item List") {
                @Override
                public Optional<String> getPagerCommand(int page) {
                    return Optional.of("/mk list -p " + page + " " + Optional.ofNullable(filterString).orElse(""));
                }

                @Override
                public Text format(MarketItemInfo info) {
                    ChatColor color = info.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED;
                    Text buy = Text.of(ChatColor.GRAY, "unavailable");
                    if (info.isBuyable() || !info.isEnabled()) {
                        buy = formatPriceForList(info.getPrice());
                    }

                    Text sell = Text.of(ChatColor.GRAY, "unavailable");
                    if (info.isSellable() || !info.isEnabled()) {
                        sell = formatPriceForList(info.getSellPrice());
                    }

                    String titleCaseName = toTitleCase(info.getUnqualifiedName());

                    Text itemName = Text.of(
                            color, info.getDisplayName(),
                            TextAction.Click.runCommand("/mk lookup " + info.getLookupName()),
                            TextAction.Hover.showText(Text.of("Show details for " + titleCaseName))
                    );

                    return Text.of(itemName,
                            ChatColor.GRAY, " x", ChatUtil.WHOLE_NUMBER_FORMATTER.format(info.getStock()),
                            ChatColor.YELLOW, " (Quick Price: ", buy, " - ", sell, ")");
                }
            }.display(sender, marketItemInfoCollection, args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"lookup", "value", "info", "pc"},
                usage = "[item name]", desc = "Value an item",
                flags = "e", min = 0)
        public void valueCmd(CommandContext args, CommandSender sender) throws CommandException {
            ItemStack stack;
            Optional<String> optItemName;
            if (args.argsLength() > 0) {
                stack = null;
                optItemName = matchItemFromNameOrId(args.getJoinedStrings(0));
            } else {
                stack = PlayerUtil.checkPlayer(sender).getInventory().getItemInHand();
                optItemName = computeItemName(stack);
            }

            if (optItemName.isEmpty()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            MarketItemLookupInstance lookupInstance = getLookupInstance(Set.of(optItemName.get()));

            Optional<MarketItem> optMarketItemInfo = lookupInstance.getItemDetails(optItemName.get());
            if (optMarketItemInfo.isEmpty()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            MarketItem marketItemInfo = optMarketItemInfo.get();

            if (!marketItemInfo.isEnabled() && !inst.hasPermission(sender, "aurora.admin.adminstore.disabled")) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            Text itemNameText = Text.of(
                    marketItemInfo.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED,
                    marketItemInfo.getDisplayName(),
                    TextAction.Hover.showItem(getBaseStack(marketItemInfo.getName()))
            );

            sender.sendMessage(Text.of(ChatColor.GOLD, "Price Information for: ", itemNameText).build());

            String stockCount = ChatUtil.WHOLE_NUMBER_FORMATTER.format(marketItemInfo.getStock());
            ChatUtil.sendNotice(sender, "There are currently " + ChatColor.GRAY + stockCount + ChatColor.YELLOW + " in stock.");

            // Purchase Information
            if (marketItemInfo.displayBuyInfo()) {
                String purchasePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(marketItemInfo.getPrice()), "");

                ChatUtil.sendNotice(sender, "When you buy it you pay:");
                ChatUtil.sendNotice(sender, " - " + purchasePrice + " each.");
            } else {
                ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be purchased.");
            }

            // Sale Information
            if (marketItemInfo.displaySellInfo()) {
                double fullyRepairedPrice = marketItemInfo.getSellPrice();
                double sellPrice = stack == null ? fullyRepairedPrice : marketItemInfo.getSellUnitPriceForStack(stack).orElse(0d);

                String sellPriceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(sellPrice), "");
                ChatUtil.sendNotice(sender, "When you sell it you get:");
                ChatUtil.sendNotice(sender, " - " + sellPriceString + " each.");

                if (fullyRepairedPrice != sellPrice) {
                    String fullyRepairedPriceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(fullyRepairedPrice), "");
                    ChatUtil.sendNotice(sender, " - " + fullyRepairedPriceString + " each when new.");
                }
            } else {
                ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be sold.");
            }

            // True value / admin information
            if (args.hasFlag('e')) {
                if (!sender.hasPermission("aurora.admin.adminstore.truevalue")) {
                    throw new CommandException("You do not have permission to see extra information about this item.");
                }

                String basePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(marketItemInfo.getValue()), "");

                ChatUtil.sendNotice(sender, "Base price:");
                ChatUtil.sendNotice(sender, " - " + basePrice + " each.");
            }

            if (marketItemInfo.isBuyable() && marketItemInfo.getStock() != 0) {
                TextBuilder builder = Text.builder();

                builder.append(ChatColor.YELLOW, "Quick buy: ");

                for (int i : List.of(1, 16, 32, 64, 128, 256)) {
                    if (i > marketItemInfo.getStock()) {
                        break;
                    }

                    if (i != 1) {
                        builder.append(", ");
                    }

                    double intervalPrice = marketItemInfo.getPrice() * i;

                    builder.append(Text.of(
                            ChatColor.BLUE,
                            TextAction.Click.runCommand("/market buy -a " + i + " " + marketItemInfo.getLookupName()),
                            TextAction.Hover.showText(Text.of(
                                    "Buy ", i, " for ", ChatUtil.TWO_DECIMAL_FORMATTER.format(intervalPrice)
                            )),
                            i
                    ));
                }

                sender.sendMessage(builder.build());
            }
        }

        @Command(aliases = {"admin"}, desc = "Administrative Commands")
        @NestedCommand({AdminStoreCommands.class})
        public void AdministrativeCommands(CommandContext args, CommandSender sender) throws CommandException {

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
                Optional<String> optItemName = matchItemFromNameOrId(item);
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
            Optional<String> optItemName = matchItemFromNameOrId(args.getJoinedStrings(0));
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
            Optional<String> optItemName = matchItemFromNameOrId(args.getJoinedStrings(0));
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

    private static ItemStack getBaseStack(String name) throws CommandException {
        try {
            if (name.startsWith("grindstone:")) {
                name = name.replaceFirst("grindstone:", "");
                CustomItems item = CustomItems.valueOf(name.toUpperCase());
                return CustomItemCenter.build(item);
            }

            NumericItem mapping = toNumeric(name).get();

            return new ItemStack(mapping.getId(), 1, mapping.getData());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new CommandException("Please report this error, " + name + " could not be found.");
        }
    }

    private static ItemStack[] getItem(String name, int amount) throws CommandException {

        List<ItemStack> itemStacks = new ArrayList<>();

        ItemStack stack = getBaseStack(name);
        for (int i = amount; i > 0;) {
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

    public String checkPlayer(CommandSender sender) throws CommandException {
        PlayerUtil.checkPlayer(sender);

        if (adminComponent.isAdmin((Player) sender)) {
            throw new CommandException("You cannot use this command while in admin mode.");
        }

        checkInArea((Player) sender);

        return sender.getName();
    }

    public void checkInArea(Player player) throws CommandException {
        if (!isInArea(player.getLocation())) {
            throw new CommandException("You call out, but no one hears your offer.");
        }
    }

    public boolean isInArea(Location location) {
        Vector v = BukkitUtil.toVector(location);
        return location.getWorld().getName().equals("City") && region != null && region.contains(v);
    }

    private WorldGuardPlugin getWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        return (WorldGuardPlugin) plugin;
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
