/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.command;

import com.google.common.base.Joiner;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.bukkittext.TextBuilder;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItem;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionBuilder;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.invgui.InventoryGUIComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.inventory.*;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.math.BigDecimal;
import java.util.*;

import static gg.packetloss.grindstone.economic.store.MarketComponent.NOT_AVAILIBLE;
import static gg.packetloss.grindstone.economic.store.MarketComponent.getLookupInstanceFromStacksImmediately;
import static gg.packetloss.grindstone.util.item.ItemNameDeserializer.getBaseStack;

@CommandContainer
public class MarketCommands {
    private final MarketComponent component;
    private final InventoryGUIComponent invGUI;
    private final WalletComponent wallet;

    public MarketCommands(MarketComponent component, InventoryGUIComponent invGUI, WalletComponent wallet) {
        this.component = component;
        this.invGUI = invGUI;
        this.wallet = wallet;
    }

    @Command(name = "buy", desc = "Buy an item")
    public void buy(Player player,
                    @Arg(desc = "amount", def = "1") int amount,
                    @Arg(desc = "item") MarketItemSet items)
            throws CommandException {
        component.checkPlayer(player);

        MarketTransactionBuilder transactionBuilder = new MarketTransactionBuilder();

        // Place a reasonable cap on the amount of items that can be purchased.
        amount = Math.min((InventoryConstants.PLAYER_INV_ROWS_TOTAL_LENGTH * 64) / items.size(), Math.max(1, amount));

        for (MarketItem item : items) {
            if (!item.isBuyable()) {
                // FIXME: Improve error message
                throw new CommandException("This item is not available for purchase.");
            }

            transactionBuilder.add(item, amount);
        }

        component.buyItems(player, transactionBuilder.toTransactionLines()).thenAcceptAsynchronously(
            (totalPrice) -> {
                ChatUtil.sendNotice(player, "Item(s) purchased for ", wallet.format(totalPrice), "!");
            },
            (error) -> {
                ChatUtil.sendError(player, error);
            }
        );
    }

    private static boolean isStorageSelectionSellMode(String itemFilter) {
        if (itemFilter.charAt(0) != '#') {
            return false;
        }

        return switch (itemFilter.substring(1).toLowerCase()) {
            case "all", "storage", "hotbar" -> true;
            default -> false;
        };
    }

    private static AbstractInventoryIterator getItemsForSellFilter(PlayerInventory pInv, String itemFilter) {
        return switch (itemFilter.substring(1).toLowerCase()) {
            case "all" -> new PlayerInventoryIterator(pInv);
            case "storage" -> new PlayerStorageInventoryIterator(pInv);
            case "hotbar" -> new PlayerHotbarInventoryIterator(pInv);
            default -> throw new IllegalStateException();
        };
    }

    @Command(name = "sell", desc = "Sell items")
    public void sell(Player player, @Arg(desc = "item filter", def = "") String itemFilter) throws CommandException {
        component.checkPlayer(player);

        Inventory sellInv = invGUI.openClosableChest(player, "Sell Items", (inv) -> {
            List<ItemStack> items = new ArrayList<>();

            inv.forEach((item) -> {
                if (item != null) {
                    items.add(item);
                }
            });

            // FIXME: This should probably be abstracted as some kind of payment calculator
            PluginTaskExecutor.submitAsync(() -> {
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
                    items.forEach((item) -> {
                        GeneralPlayerUtil.giveItemToPlayer(player, item);
                    });

                    if (transactionBuilder.isEmpty()) {
                        ChatUtil.sendError(player, "No sellable items found!");
                        return;
                    }

                    BigDecimal finalPayment = new BigDecimal(payment);
                    component.sellItems(player, transactionBuilder.toTransactionLines(), finalPayment).thenAccept(
                        (newBalance) -> {
                            ChatUtil.sendNotice(player, "Item(s) sold for: ", wallet.format(finalPayment), "!");
                        },
                        (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
                    );
                });
            });
        });

        if (itemFilter != null) {
            PlayerInventory playerInv = player.getInventory();

            boolean isStorageSelectionMode = isStorageSelectionSellMode(itemFilter);

            AbstractInventoryIterator itemsIt = isStorageSelectionMode ? getItemsForSellFilter(playerInv, itemFilter)
                                                                       : new PlayerInventoryIterator(playerInv);
            while (itemsIt.hasNext()) {
                ItemStack item = itemsIt.next();
                if (item == null) {
                    continue;
                }

                if (!isStorageSelectionMode) {
                    Optional<String> itemName = ItemNameCalculator.computeItemName(item);
                    if (itemName.isEmpty()) {
                        continue;
                    }

                    String unqualifiedName = ItemNameCalculator.getUnqualifiedName(itemName.get());
                    if (!unqualifiedName.contains(itemFilter.toLowerCase())) {
                        continue;
                    }
                }

                itemsIt.clear();
                ItemStack remainder = sellInv.addItem(item).get(0);
                if (remainder != null) {
                    itemsIt.set(remainder);
                    break;
                }
            }
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
                     @Arg(name = "itemFilter", desc = "item filter", def = "", variable = true) List<String> itemFilterArgs) {
        String itemFilter = Joiner.on(' ').join(itemFilterArgs);

        component.asyncGetItemListFor(sender, itemFilter).thenApplyAsynchronously((itemCollection) -> {
            Collections.sort(itemCollection);
            return itemCollection;
        }).thenAcceptAsynchronously((itemCollection) -> {
            new TextComponentChatPaginator<MarketItem>(ChatColor.GOLD, "Item List") {
                @Override
                public Optional<String> getPagerCommand(int page) {
                    return Optional.of("/market list -p " + page + " " + itemFilter);
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
                        color, info.getDisplayNameNoColor(),
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
        });
    }

    private void printDetails(
            CommandSender sender, MarketItem item, ItemStack stack, boolean showExtra) {
        Text itemNameText = Text.of(
                item.isEnabled() ? ChatColor.BLUE : ChatColor.DARK_RED,
                item.getDisplayNameNoColor(),
                TextAction.Hover.showItem(getBaseStack(item.getName()))
        );

        sender.sendMessage(Text.of(ChatColor.GOLD, "Price Information for: ", itemNameText).build());

        String stockCount = ChatUtil.WHOLE_NUMBER_FORMATTER.format(item.getStock());
        ChatUtil.sendNotice(sender, "There are currently " + ChatColor.GRAY + stockCount + ChatColor.YELLOW + " in stock.");

        // Purchase Information
        if (item.displayBuyInfo()) {
            ChatUtil.sendNotice(sender, "When you buy it you pay:");
            ChatUtil.sendNotice(sender, " - ", wallet.format(item.getPrice()), " each.");
        } else {
            ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be purchased.");
        }

        // Sale Information
        if (item.displaySellInfo()) {
            double fullyRepairedPrice = item.getSellPrice();
            double sellPrice = stack == null ? fullyRepairedPrice : item.getSellUnitPriceForStack(stack).orElse(0d);

            ChatUtil.sendNotice(sender, "When you sell it you get:");
            ChatUtil.sendNotice(sender, " - ", wallet.format(sellPrice), " each.");

            if (fullyRepairedPrice != sellPrice) {
                ChatUtil.sendNotice(sender, " - " + wallet.format(fullyRepairedPrice) + " each when new.");
            }
        } else {
            ChatUtil.sendNotice(sender, ChatColor.GRAY, "This item cannot be sold.");
        }

        // True value / admin information
        if (showExtra) {
            ChatUtil.sendNotice(sender, "Base price:");
            if (sender.hasPermission("aurora.admin.adminstore.truevalue")) {
                ChatUtil.sendNotice(sender, " - " + wallet.format(item.getValue()) + " each.");
            } else {
                ChatUtil.sendError(sender, " - permission denied.");
            }
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
                                "Sell " + item.getDisplayNameNoColor() + " using a GUI"
                        )),
                        "PICK"
                ));
                builder.append(" -- ");
                builder.append(Text.of(
                        ChatColor.BLUE,
                        TextAction.Click.runCommand("/market sellgui " + item.getName()),
                        TextAction.Hover.showText(Text.of(
                                "Sell all " + item.getDisplayNameNoColor() + " using a GUI"
                        )),
                        "ALL"
                ));

                sender.sendMessage(builder.build());
            }
        }
    }

    @Command(name = "lookup", desc = "Value an item")
    public void lookup(CommandSender sender, @Switch(name = 'e', desc = "extra") boolean extra,
                       @Arg(desc = "item filter") MarketItem item) {
        printDetails(sender, item, null, extra);
    }

    @Command(name = "pricecheck", desc = "Price check your held")
    public void lookup(Player player, @Switch(name = 'e', desc = "extra") boolean extra) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            ChatUtil.sendError(player, "Hold an item to price check.");
            return;
        }

        MarketComponent.getLookupInstanceFromStacks(List.of(held)).thenAccept((lookupInstance -> {
            Optional<MarketItem> itemInfo = lookupInstance.getItemDetails(held);
            if (itemInfo.isEmpty()) {
                ChatUtil.sendError(player, NOT_AVAILIBLE);
                return;
            }

            printDetails(player, itemInfo.get(), held, extra);
        }));
    }
}
