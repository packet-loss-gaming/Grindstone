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
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.store.mysql.MySQLItemStoreDatabase;
import gg.packetloss.grindstone.economic.store.mysql.MySQLMarketTransactionDatabase;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.item.InventoryUtil.InventoryView;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.legacy.ItemType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;

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

    private DecimalFormat wholeNumberFormatter = new DecimalFormat("#,###");
    private DecimalFormat oneDecimalFormatter = new DecimalFormat("#,###.#");

    public void simulateMarket() {
        itemDatabase.updatePrices();

        ChatUtil.sendNotice(server.getOnlinePlayers(), ChatColor.GOLD + "The market has been updated!");
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

            Optional<String> optItemName = matchItemFromNameOrId(args.getJoinedStrings(0).toLowerCase());
            if (optItemName.isEmpty()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            String itemName = optItemName.get();
            MarketItemInfo marketItemInfo = itemDatabase.getItem(itemName);
            if (marketItemInfo == null || !marketItemInfo.isBuyable()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            itemName = marketItemInfo.getName();

            int amt = 1;
            if (args.hasFlag('a')) {
                amt = Math.max(1, args.getFlagInteger('a'));
            }

            if (amt > marketItemInfo.getStock()) {
                throw new CommandException("You requested " + wholeNumberFormatter.format(amt) + " however, only "
                  + wholeNumberFormatter.format(marketItemInfo.getStock()) + " are in stock.");
            }

            double price = marketItemInfo.getPrice() * amt;
            double lottery = price * .03;

            if (!econ.has(playerName, price)) {
                throw new CommandException("You do not have enough money to purchase that item(s).");
            }

            // Get the items and add them to the inventory
            ItemStack[] itemStacks = getItem(marketItemInfo.getName(), amt);
            for (ItemStack itemStack : itemStacks) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), itemStack);
                    continue;
                }
                player.getInventory().addItem(itemStack);
            }

            // Deposit into the lottery account
            econ.bankDeposit("Lottery", lottery);

            // Charge the money and send the sender some feedback
            econ.withdrawPlayer(playerName, price);

            // Update market stocks.
            HashMap<String, Integer> adjustments = new HashMap<>();
            adjustments.put(itemName, -amt);
            itemDatabase.adjustStocks(adjustments);

            transactionDatabase.logTransaction(playerName, itemName, amt);
            transactionDatabase.save();
            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), "");
            ChatUtil.sendNotice(sender, "Item(s) purchased for " + priceString + "!");

            // Market Command Help
            StoreSession sess = sessions.getSession(StoreSession.class, player);
            if (amt == 1 && sess.recentPurch() && sess.getLastPurch().equals(itemName)) {
                ChatUtil.sendNotice(sender, "Did you know you can specify the amount of items to buy?");
                ChatUtil.sendNotice(sender, "/market buy -a <amount> " + marketItemInfo.getDisplayName());
            }
            sess.setLastPurch(itemName);
        }

        @Command(aliases = {"sell", "s"},
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
                verifyValidItemCommand(filter);
            }

            InventoryView view = new InventoryView(min, max, filter);

            Set<String> itemNamesInFilter = new HashSet<>();

            view.operateOnInventory(itemStacks, (item) -> {
                computeItemName(item).ifPresent(itemNamesInFilter::add);
                return item;
            });

            // FIXME: This is a rat's nest.
            server.getScheduler().runTaskAsynchronously(inst, () -> {
                Map<String, MarketItemInfo> nameItemMapping = itemDatabase.getItems(itemNamesInFilter);
                server.getScheduler().runTask(inst, () -> {
                    double[] payment = {0}; // hack to update payment from inside lambda
                    HashMap<String, Integer> transactions = new HashMap<>();

                    // Calculate the payment and transactions using the new inventory.
                    ItemStack[] newInventory = player.getInventory().getContents();
                    view.operateOnInventory(newInventory, (item) -> {
                        Optional<Double> optPercentageSale = computePercentageSale(item);
                        if (optPercentageSale.isEmpty()) {
                            return item;
                        }

                        Optional<String> optItemName = computeItemName(item);
                        if (optItemName.isEmpty()) {
                            return item;
                        }

                        final double percentageSale = optPercentageSale.get();
                        final String itemName = optItemName.get();

                        MarketItemInfo marketItemInfo = nameItemMapping.get(itemName.toUpperCase());
                        if (marketItemInfo == null || !marketItemInfo.isSellable()) {
                            return item;
                        }

                        int amt = item.getAmount();
                        payment[0] += marketItemInfo.getSellPrice() * amt * percentageSale;

                        transactions.merge(itemName, amt, (oldItemName, oldAmt) -> oldAmt + amt);

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

        private String formatPriceForList(double price) {
            String result = "";
            result += ChatColor.WHITE;
            if (price >= 10000) {
                result += "~" + wholeNumberFormatter.format(price / 1000) + "k";
            } else if (price >= 1000) {
                result += "~" + oneDecimalFormatter.format(price / 1000) + "k";
            } else {
                result += econ.format(price);
            }
            result += ChatColor.YELLOW;
            return result;
        }

        @Command(aliases = {"list", "l"},
                usage = "[-p page] [filter...]", desc = "Get a list of items and their prices",
                flags = "p:", min = 0
        )
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {

            String filterString = args.argsLength() > 0 ? args.getJoinedStrings(0) : null;
            List<MarketItemInfo> marketItemInfoCollection = itemDatabase.getItemList(filterString,
                    inst.hasPermission(sender, "aurora.admin.adminstore.disabled"));
            Collections.sort(marketItemInfoCollection);

            new PaginatedResult<MarketItemInfo>(ChatColor.GOLD + "Item List") {
                @Override
                public String format(MarketItemInfo info) {
                    ChatColor color = info.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED;
                    String buy, sell;
                    if (info.isBuyable() || !info.isEnabled()) {
                        buy = formatPriceForList(info.getPrice());
                    } else {
                        buy = ChatColor.GRAY + "unavailable" + ChatColor.YELLOW;
                    }
                    if (info.isSellable() || !info.isEnabled()) {
                        sell = formatPriceForList(info.getSellPrice());
                    } else {
                        sell = ChatColor.GRAY + "unavailable" + ChatColor.YELLOW;
                    }

                    String message = color + info.getDisplayName()
                            + ChatColor.GRAY + " x" + wholeNumberFormatter.format(info.getStock())
                            + ChatColor.YELLOW + " (Quick Price: " + buy + " - " + sell + ")";
                    return message.replace(' ' + econ.currencyNamePlural(), "");
                }
            }.display(sender, marketItemInfoCollection, args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"lookup", "value", "info", "pc"},
                usage = "[item name]", desc = "Value an item",
                flags = "", min = 0)
        public void valueCmd(CommandContext args, CommandSender sender) throws CommandException {
            String itemName;
            double percentageSale = 1;
            if (args.argsLength() > 0) {
                Optional<String> optItemName = matchItemFromNameOrId(args.getJoinedStrings(0).toLowerCase());
                if (optItemName.isEmpty()) {
                    throw new CommandException(NOT_AVAILIBLE);
                }

                itemName = optItemName.get();
            } else {
                ItemStack stack = PlayerUtil.checkPlayer(sender).getInventory().getItemInHand();
                verifyValidItemCommand(stack);

                Optional<String> optItemName = computeItemName(stack);
                if (optItemName.isPresent()) {
                    itemName = optItemName.get();
                } else {
                    throw new CommandException(NOT_AVAILIBLE);
                }

                Optional<Double> optPercentageSale = computePercentageSale(stack);
                if (optPercentageSale.isPresent()) {
                    percentageSale = optPercentageSale.get();
                } else {
                    throw new CommandException(NOT_AVAILIBLE);
                }
            }

            MarketItemInfo marketItemInfo = itemDatabase.getItem(itemName);
            if (marketItemInfo == null) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            if (!marketItemInfo.isEnabled() && !inst.hasPermission(sender, "aurora.admin.adminstore.disabled")) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            ChatColor color = marketItemInfo.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED;
            double paymentPrice = marketItemInfo.getSellPrice() * percentageSale;

            ChatUtil.sendNotice(sender, ChatColor.GOLD, "Price Information for: " + color + marketItemInfo.getDisplayName());

            String stockCount = wholeNumberFormatter.format(marketItemInfo.getStock());
            ChatUtil.sendNotice(sender, "There are currently " + ChatColor.GRAY + stockCount + ChatColor.YELLOW + " in stock.");

            // Purchase Information
            if (marketItemInfo.isBuyable() || !marketItemInfo.isEnabled()) {
                String purchasePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(marketItemInfo.getPrice()), "");

                ChatUtil.sendNotice(sender, "When you buy it you pay:");
                ChatUtil.sendNotice(sender, " - " + purchasePrice + " each.");
            } else {
                ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be purchased.");
            }

            // Sale Information
            if (marketItemInfo.isSellable() || !marketItemInfo.isEnabled()) {
                String sellPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(paymentPrice), "");

                ChatUtil.sendNotice(sender, "When you sell it you get:");
                ChatUtil.sendNotice(sender, " - " + sellPrice + " each.");
                if (percentageSale != 1.0) {
                    sellPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(marketItemInfo.getSellPrice()), "");
                    ChatUtil.sendNotice(sender, " - " + sellPrice + " each when new.");
                }
            } else {
                ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be sold.");
            }

            // True value / admin information
            if (sender.hasPermission("aurora.admin.adminstore.truevalue")) {
                String basePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(marketItemInfo.getValue()), "");

                ChatUtil.sendNotice(sender, "Base price:");
                ChatUtil.sendNotice(sender, " - " + basePrice + " each.");
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
          flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.admin.adminstore.simulate")
        public void simulateCmd(CommandContext args, CommandSender sender) throws CommandException {
            server.getScheduler().runTaskAsynchronously(inst, MarketComponent.this::simulateMarket);
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

    private static Set<Integer> ignored = new HashSet<>();

    static {
        ignored.add(BlockID.AIR);
        ignored.add(BlockID.WATER);
        ignored.add(BlockID.STATIONARY_WATER);
        ignored.add(BlockID.LAVA);
        ignored.add(BlockID.STATIONARY_LAVA);
        ignored.add(BlockID.GRASS);
        ignored.add(BlockID.DIRT);
        ignored.add(BlockID.GRAVEL);
        ignored.add(BlockID.SAND);
        ignored.add(BlockID.SANDSTONE);
        ignored.add(BlockID.SNOW);
        ignored.add(BlockID.SNOW_BLOCK);
        ignored.add(BlockID.STONE);
        ignored.add(BlockID.BEDROCK);
    }

    private static boolean verifyValidItemStack(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR;
    }


    private static void verifyValidItemCommand(ItemStack stack) throws CommandException {
        if (!verifyValidItemStack(stack)) {
            throw new CommandException("That's not a valid item!");
        }

        if (!verifyValidCustomItem(stack)) {
            throw new CommandException("Renamed items are unsupported!");
        }

    }

    private static Optional<Double> computePercentageSale(ItemStack stack) {
        double percentageSale = 1;
        if (stack.getDurability() != 0 && !ItemType.usesDamageValue(stack.getTypeId())) {
            if (stack.getAmount() > 1) {
                return Optional.empty();
            }
            percentageSale = 1 - ((double) stack.getDurability() / (double) stack.getType().getMaxDurability());
        }
        return Optional.of(percentageSale);
    }

    public static double priceCheck(int blockID, int data) {
        if (ignored.contains(blockID)) return 0;

        Optional<String> optBlockName = ItemNameCalculator.computeBlockName(blockID, data);
        if (optBlockName.isEmpty()) return 0;

        MarketItemInfo marketItemInfo = itemDatabase.getItem(optBlockName.get());
        if (marketItemInfo == null) return 0;

        return marketItemInfo.getPrice();
    }

    /**
     * Price checks an item stack
     *
     * @param stack the item stack to be price checked
     * @return -1 if invalid, otherwise returns the price scaled to item stack quantity
     */
    public static double priceCheck(ItemStack stack) {
        return priceCheck(stack, true);
    }

    public static double priceCheck(ItemStack stack, boolean percentDamage) {
        if (!verifyValidItemStack(stack)) {
            return -1;
        }

        Optional<Double> optPercentageSale = percentDamage ? computePercentageSale(stack) : Optional.of(1D);
        if (optPercentageSale.isEmpty()) {
            return -1;
        }

        Optional<String> optItemName = computeItemName(stack);
        if (optItemName.isEmpty()) {
            return -1;
        }

        final double percentageSale = optPercentageSale.get();
        final String itemName = optItemName.get();

        MarketItemInfo marketItemInfo = itemDatabase.getItem(itemName);
        if (marketItemInfo == null) {
            return -1;
        }

        return marketItemInfo.getPrice() * percentageSale * stack.getAmount();
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
