/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil.PlayerParser;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Lost Custom Items", desc = "Lost item commands.")
public class LostItemsComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"/give"}, usage = "[player] <item> [amount]", desc = "Custom Item give command", min = 0, max = 3)
        @CommandPermissions({"aurora.lostitems"})
        public void lostItemCommands(CommandContext args, CommandSender sender) throws CommandException {
            Player player = null;
            String itemName = "";
            int amount = 1;

            if (args.argsLength() == 3) {
                player = PlayerParser.matchSinglePlayer(sender, args.getString(0));
                itemName = args.getString(1);
                amount = args.getInteger(2);
            } else if (args.argsLength() == 2) {
                player = PlayerParser.matchSinglePlayer(sender, args.getString(0));
                itemName = args.getString(1);
            } else if (args.argsLength() == 1) {
                player = PlayerUtil.checkPlayer(sender);
                itemName = args.getString(0);
            }

            CustomItems item;
            try {
                try {
                    int itemId = Integer.parseInt(itemName);
                    item = CustomItems.values()[itemId];
                } catch (NumberFormatException ex) {
                    item = CustomItems.valueOf(itemName);
                }
            } catch (ArrayIndexOutOfBoundsException|IllegalArgumentException ex) {
                ChatUtil.sendNotice(sender, ChatColor.GOLD + "Valid items:");
                int counter = 0;
                for (CustomItems aItem : CustomItems.values()) {
                    ChatUtil.sendNotice(sender, aItem.getColor() + "" + counter++ + "). " + aItem.getName());
                }

                return;
            }

            Validate.notNull(player);

            ItemStack stack = CustomItemCenter.build(item);
            amount = Math.max(1, Math.min(amount, stack.getMaxStackSize()));
            stack.setAmount(amount);

            player.getInventory().addItem(stack);

            if (player.equals(sender)) {
                ChatUtil.sendNotice(sender, "You've been given " + amount + " " +
                        item.getColoredName() + ChatColor.YELLOW + ".");
            } else {
                ChatUtil.sendNotice(sender, "Gave " + amount + " " +
                        item.getColoredName() + ChatColor.YELLOW + " to " + player.getName() + ".");
            }
        }
    }
}
