/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.command;

import com.google.common.base.Joiner;
import com.sk89q.commandbook.command.argument.OfflineSinglePlayerTarget;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.economic.store.ItemTransaction;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItem;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.matchItem;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class MarketAdminCommands {
    private final MarketComponent component;
    private final WalletComponent wallet;

    public MarketAdminCommands(MarketComponent component, WalletComponent wallet) {
        this.component = component;
        this.wallet = wallet;
    }

    @Command(name = "log", desc = "Item database logs")
    @CommandPermissions("aurora.admin.adminstore.log")
    public void logCmd(CommandSender sender,
                       @ArgFlag(name = 'i', desc = "item", def = "") MarketItem item,
                       @ArgFlag(name = 'u', desc = "user", def = "") OfflineSinglePlayerTarget player,
                       @ArgFlag(name = 'p', desc = "page", def = "1") int page) {
        String itemName = item == null ? null : item.getName();
        UUID playerID = player == null ? null : player.get().getUniqueId();

        component.getTransactions(itemName, playerID).thenAcceptAsynchronously((transactions) -> {
            new TextComponentChatPaginator<ItemTransaction>(ChatColor.GOLD, "Item List") {
                @Override
                public Optional<String> getPagerCommand(int page) {
                    StringBuilder command = new StringBuilder();
                    command.append("/market admin log -p ").append(page);
                    if (itemName != null) {
                        command.append(" -i ").append(itemName);
                    }
                    if (playerID != null) {
                        command.append(" -u ").append(playerID);
                    }
                    return Optional.of(command.toString());
                }

                private Text getTransactionTypeText(ItemTransaction trans) {
                    if (trans.getAmount() > 0) {
                        return Text.of(ChatColor.RED, "bought");
                    } else {
                        return Text.of(ChatColor.DARK_GREEN, "sold");
                    }
                }

                @Override
                public Text format(ItemTransaction trans) {
                    return Text.of(
                        ChatColor.YELLOW,
                        trans.getPlayer(),
                        ' ',
                        getTransactionTypeText(trans),
                        ' ',
                        Math.abs(trans.getAmount()),
                        ' ',
                        Text.of(ChatColor.BLUE, trans.getItem().getDisplayName())
                    );
                }
            }.display(sender, transactions, page);
        });
    }

    @Command(name = "scale", desc = "Scale the item database")
    @CommandPermissions("aurora.admin.adminstore.scale")
    public void scaleCmd(CommandSender sender, @Arg(name = "factor", desc = "scale amount") double factor) throws CommandException {
        if (factor <= 0) {
            throw new CommandException("Cannot scale by 0 or a negative factor.");
        }

        component.scaleMarket(factor).thenAcceptAsynchronously((ignored) -> {
            ChatUtil.sendNotice(sender, "Market Scaled by: " + factor + ".");
        });
    }

    private String getItemChecked(String item) throws CommandException {
        Optional<String> optItemName = matchItem(item);
        if (optItemName.isPresent()) {
            return optItemName.get();
        } else {
            throw new CommandException("No item by that name was found.");
        }
    }

    @Command(name = "add", desc = "Add an item to the database")
    @CommandPermissions("aurora.admin.adminstore.add")
    public void addCmd(CommandSender sender,
                       @Switch(name = 'b', desc = "disable buy") boolean disableBuy,
                       @Switch(name = 's', desc = "disable sell") boolean disableSell,
                       @Arg(name = "price", desc = "item price") double price,
                       @Arg(name = "itemName", desc = "item name", variable = true) List<String> itemNameParts) throws CommandException {
        if (price < .01) {
            throw new CommandException("Item price must be at least 0.01");
        }

        String itemName = getItemChecked(Joiner.on(' ').join(itemNameParts));

        // Database operations
        component.addItem(itemName, price, disableBuy, disableSell).thenAcceptAsynchronously((oldItem) -> {
            String noticeString = oldItem == null ? " added with a price of " : " is now ";
            ChatUtil.sendNotice(sender, ChatColor.BLUE, itemName.toUpperCase(), ChatColor.YELLOW, noticeString, wallet.format(price), "!");
            if (disableBuy) {
                ChatUtil.sendNotice(sender, " - It cannot be purchased.");
            }
            if (disableSell) {
                ChatUtil.sendNotice(sender, " - It cannot be sold.");
            }
        });
    }

    @Command(name = "remove", desc = "Remove an item from the database")
    @CommandPermissions("aurora.admin.adminstore.remove")
    public void removeCmd(CommandSender sender, @Arg(desc = "item filter") MarketItem item) {
        // Database operations
        component.removeItem(item.getName()).thenAcceptAsynchronously((ignored) -> {
            ChatUtil.sendNotice(sender, ChatColor.BLUE + item.getDisplayName() + ChatColor.YELLOW + " has been removed from the database!");
        });
    }

    @Command(name = "simulate", desc = "Simulate market activity")
    @CommandPermissions("aurora.admin.adminstore.simulate")
    public void simulateCmd(CommandSender sender,
                            @Arg(name = "rounds", desc = "number of times to simulate", def = "1") int rounds)
            throws CommandException {
        if (rounds < 1) {
            throw new CommandException("The market must be simulated for at least 1 round");
        }

        PluginTaskExecutor.submitAsync(() -> {
            component.simulateMarket(rounds);
        });
    }
}
