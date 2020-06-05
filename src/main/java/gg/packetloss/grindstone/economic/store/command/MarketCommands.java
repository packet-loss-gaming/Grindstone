package gg.packetloss.grindstone.economic.store.command;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.bukkittext.TextBuilder;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItem;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionBuilder;
import gg.packetloss.grindstone.invgui.InventoryGUIComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.inventory.InventoryConstants;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.*;

import static gg.packetloss.grindstone.economic.store.MarketComponent.getBaseStack;
import static gg.packetloss.grindstone.economic.store.MarketComponent.getLookupInstanceFromStacksImmediately;

@CommandContainer
public class MarketCommands {
    private MarketComponent component;
    private InventoryGUIComponent invGUI;
    private Economy economy;

    public MarketCommands(MarketComponent component, InventoryGUIComponent invGUI, Economy economy) {
        this.component = component;
        this.invGUI = invGUI;
        this.economy = economy;
    }

    @Command(name = "buy", desc = "Buy an item")
    public void buy(Player player,
                    @Arg(desc = "amount", def = "1") int amount,
                    @Arg(desc = "item") MarketItemSet items)
            throws CommandException {
        amount = Math.min(InventoryConstants.PLAYER_INV_STORAGE_LENGTH, Math.max(1, amount));

        MarketTransactionBuilder transactionBuilder = new MarketTransactionBuilder();

        for (MarketItem item : items) {
            if (!item.isBuyable()) {
                // FIXME: Improve error message
                throw new CommandException("This item is not available for purchase.");
            }

            transactionBuilder.add(item, amount);
        }

        double totalPrice = component.buyItems(player, transactionBuilder.toTransactionLines());

        String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(totalPrice), "");
        ChatUtil.sendNotice(player, "Item(s) purchased for " + priceString + "!");
    }

    @Command(name = "sell", desc = "Sell items")
    public void sell(Player player, @Arg(desc = "item filter", def = "") String itemFilter) {
        Inventory sellInv = invGUI.openClosableChest(player, "Sell Items", (inv) -> {
            List<ItemStack> items = new ArrayList<>();

            inv.forEach((item) -> {
                if (item != null) {
                    items.add(item);
                }
            });

            // FIXME: This should probably be abstracted as some kind of payment calculator
            CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
                MarketItemLookupInstance lookupInstance = getLookupInstanceFromStacksImmediately(items);
                CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                    MarketTransactionBuilder transactionBuilder = new MarketTransactionBuilder();
                    double payment = 0;

                    // Calculate the payment and transactions for these items
                    Iterator<ItemStack> it = items.iterator();
                    while (it.hasNext()) {
                        ItemStack item = it.next();

                        Optional<MarketItem> optMarketItem = lookupInstance.getItemDetails(item);
                        if (optMarketItem.isEmpty()) {
                            continue;
                        }

                        MarketItem marketItem = optMarketItem.get();
                        if (!marketItem.isSellable()) {
                            continue;
                        }

                        Optional<Double> optSellPrice = marketItem.getSellPriceForStack(item);
                        if (optSellPrice.isEmpty()) {
                            continue;
                        }

                        transactionBuilder.add(marketItem, item.getAmount());
                        payment += optSellPrice.get();

                        it.remove();
                    }

                    // Add anything that couldn't be sold back to the inventory, or drop it on the ground
                    Inventory playerInv = player.getInventory();
                    items.forEach((item) -> {
                        ItemStack remainder = playerInv.addItem(item).get(0);
                        if (remainder != null) {
                            player.getWorld().dropItem(player.getLocation(), remainder);
                        }
                    });

                    if (transactionBuilder.isEmpty()) {
                        ChatUtil.sendError(player, "No sellable items found!");
                        return;
                    }

                    component.sellItems(player, transactionBuilder.toTransactionLines(), payment);

                    String paymentString = ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(payment), "");
                    ChatUtil.sendNotice(player, "Item(s) sold for: " + paymentString + "!");
                });
            });
        });

        if (itemFilter != null) {
            Inventory playerInv = player.getInventory();

            ItemStack[] contents = playerInv.getStorageContents();
            for (int i = 0; i < contents.length; ++i) {
                ItemStack item = contents[i];
                Optional<String> itemName = ItemNameCalculator.computeItemName(item);
                if (itemName.isEmpty()) {
                    continue;
                }

                String unqualifiedName = ItemNameCalculator.getUnqualifiedName(itemName.get());
                if (unqualifiedName.contains(itemFilter.toLowerCase())) {
                    contents[i] = null;
                    ItemStack remainder = sellInv.addItem(item).get(0);
                    if (remainder != null) {
                        contents[i] = remainder;
                        break;
                    }
                }
            }

            playerInv.setStorageContents(contents);
        }
    }
    private Text formatPriceForList(double price) {
        String result = "";
        result += ChatColor.WHITE;

        boolean fullPriceShown = false;
        if (price >= 10000) {
            result += "~" + ChatUtil.WHOLE_NUMBER_FORMATTER.format(price / 1000) + "k";
        } else if (price >= 1000) {
            result += "~" + ChatUtil.ONE_DECIMAL_FORMATTER.format(price / 1000) + "k";
        } else {
            fullPriceShown = true;
            result += ChatUtil.TWO_DECIMAL_FORMATTER.format(price);
        }

        if (fullPriceShown) {
            return Text.of(result);
        }

        return Text.of(
                result,
                TextAction.Hover.showText(Text.of(ChatUtil.TWO_DECIMAL_FORMATTER.format(price)))
        );
    }

    @Command(name = "list", desc = "Get a list of items and their prices")
    public void list(CommandSender sender, @ArgFlag(name = 'p', desc = "page", def = "1") int page,
                     @Arg(desc = "item filter", def = "") String itemFilter) throws CommandException {
        List<MarketItem> itemCollection = component.getItemListFor(sender, itemFilter);
        Collections.sort(itemCollection);

        new TextComponentChatPaginator<MarketItem>(ChatColor.GOLD, "Item List") {
            @Override
            public Optional<String> getPagerCommand(int page) {
                return Optional.of("/market list -p " + page + " " + Optional.ofNullable(itemFilter).orElse(""));
            }

            @Override
            public Text format(MarketItem info) {
                ChatColor color = info.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED;
                Text buy = Text.of(ChatColor.GRAY, "unavailable");
                if (info.isBuyable() || !info.isEnabled()) {
                    buy = formatPriceForList(info.getPrice());
                }

                Text sell = Text.of(ChatColor.GRAY, "unavailable");
                if (info.isSellable() || !info.isEnabled()) {
                    sell = formatPriceForList(info.getSellPrice());
                }

                Text itemName = Text.of(
                        color, info.getDisplayName(),
                        TextAction.Click.runCommand("/market lookup " + info.getLookupName()),
                        TextAction.Hover.showText(Text.of("Show details for " + info.getTitleCasedName()))
                );

                return Text.of(
                        itemName,
                        ChatColor.GRAY, " x", ChatUtil.WHOLE_NUMBER_FORMATTER.format(info.getStock()),
                        ChatColor.YELLOW, " (",
                        ChatColor.DARK_GREEN, "B: ", buy,
                        ChatColor.YELLOW, ", ",
                        ChatColor.DARK_GREEN, "S: ", sell,
                        ChatColor.YELLOW, ")"
                );
            }
        }.display(sender, itemCollection, page);
    }

    // FIXME: Add support for using the held item again
    @Command(name = "lookup", desc = "Value an item")
    public void lookup(CommandSender sender, @Switch(name = 'e', desc = "extra") boolean extra,
                       @Arg(desc = "item filter") MarketItem item) throws CommandException {
        Text itemNameText = Text.of(
                item.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED,
                item.getDisplayName(),
                TextAction.Hover.showItem(getBaseStack(item.getName()))
        );

        sender.sendMessage(Text.of(ChatColor.GOLD, "Price Information for: ", itemNameText).build());

        String stockCount = ChatUtil.WHOLE_NUMBER_FORMATTER.format(item.getStock());
        ChatUtil.sendNotice(sender, "There are currently " + ChatColor.GRAY + stockCount + ChatColor.YELLOW + " in stock.");

        // Purchase Information
        if (item.displayBuyInfo()) {
            String purchasePrice = ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(item.getPrice()), "");

            ChatUtil.sendNotice(sender, "When you buy it you pay:");
            ChatUtil.sendNotice(sender, " - " + purchasePrice + " each.");
        } else {
            ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be purchased.");
        }

        // Sale Information
        if (item.displaySellInfo()) {
            double fullyRepairedPrice = item.getSellPrice();
            double sellPrice = /*stack == null ? */fullyRepairedPrice/* : item.getSellUnitPriceForStack(stack).orElse(0d)*/;

            String sellPriceString = ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(sellPrice), "");
            ChatUtil.sendNotice(sender, "When you sell it you get:");
            ChatUtil.sendNotice(sender, " - " + sellPriceString + " each.");

            if (fullyRepairedPrice != sellPrice) {
                String fullyRepairedPriceString = ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(fullyRepairedPrice), "");
                ChatUtil.sendNotice(sender, " - " + fullyRepairedPriceString + " each when new.");
            }
        } else {
            ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be sold.");
        }

        // True value / admin information
        if (extra) {
            if (!sender.hasPermission("aurora.admin.adminstore.truevalue")) {
                throw new CommandException("You do not have permission to see extra information about this item.");
            }

            String basePrice = ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(item.getValue()), "");

            ChatUtil.sendNotice(sender, "Base price:");
            ChatUtil.sendNotice(sender, " - " + basePrice + " each.");
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (item.isBuyable() && item.getStock() != 0) {
                TextBuilder builder = Text.builder();

                builder.append(ChatColor.YELLOW, "Quick buy: ");

                for (int i : List.of(1, 16, 32, 64, 128, 256)) {
                    if (i > item.getStock()) {
                        break;
                    }

                    if (i != 1) {
                        builder.append(", ");
                    }

                    double intervalPrice = item.getPrice() * i;

                    builder.append(Text.of(
                            ChatColor.BLUE,
                            TextAction.Click.runCommand("/market buy " + i + " " + item.getName()),
                            TextAction.Hover.showText(Text.of(
                                    "Buy ", i, " for ", ChatUtil.TWO_DECIMAL_FORMATTER.format(intervalPrice)
                            )),
                            i
                    ));
                }

                sender.sendMessage(builder.build());
            }

            ItemStack[] contents = player.getInventory().getStorageContents();
            int itemCount = ItemUtil.countItemsOfComputedName(contents, item.getName());
            if (item.isSellable() && itemCount > 0) {
                TextBuilder builder = Text.builder();

                builder.append(ChatColor.YELLOW, "Quick sell: ");

                builder.append(Text.of(
                        ChatColor.BLUE,
                        TextAction.Click.runCommand("/market sellgui"),
                        TextAction.Hover.showText(Text.of(
                                "Sell " + item.getDisplayName() + " using a GUI"
                        )),
                        "PICK"
                ));
                builder.append(" -- ");
                builder.append(Text.of(
                        ChatColor.BLUE,
                        TextAction.Click.runCommand("/market sellgui " + item.getName()),
                        TextAction.Hover.showText(Text.of(
                                "Sell all " + item.getDisplayName() + " using a GUI"
                        )),
                        "ALL"
                ));

                sender.sendMessage(builder.build());
            }
        }
    }

}
