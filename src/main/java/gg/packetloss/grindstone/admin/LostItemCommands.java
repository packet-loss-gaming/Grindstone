/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.command.argument.SinglePlayerTarget;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.util.*;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class LostItemCommands {
    private static void giveItem(CommandSender sender, Player player, CustomItems item, int amount) {
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

    @Command(name = "/give", desc = "Custom Item give command")
    @CommandPermissions({"aurora.lostitems"})
    public void giveCmd(CommandSender sender,
                        @Arg(desc = "items", def = "") CustomItemBundle customItems,
                        @Arg(desc = "count", def = "1") int count,
                        @Arg(desc = "target", def = "") SinglePlayerTarget player) {
        if (customItems == null) {
            ChatUtil.sendNotice(sender, ChatColor.GOLD + "Valid items:");
            int counter = 0;
            for (CustomItems aItem : CustomItems.values()) {
                sender.sendMessage(Text.of(
                        aItem.getColor(),
                        counter++,
                        "). ", aItem.getSnakecaseName().toUpperCase(),
                        TextAction.Click.runCommand("//give " + aItem.getSnakecaseName()),
                        TextAction.Hover.showText(Text.of("Give self ", aItem.getName()))
                ).build());
            }
            return;
        }

        if (player == null) {
            if (sender instanceof Player) {
                player = new SinglePlayerTarget((Player) sender);
            } else {
                ChatUtil.sendError(sender, "A target must be specified.");
                return;
            }
        }

        // Map all to verify via exceptions first
        List<CustomItems> items = customItems.getItems();

        // Give resolved items
        Player targetPlayer = player.get();
        items.forEach((item) -> {
            giveItem(sender, targetPlayer, item, count);
        });
    }

    @Command(name = "/itemhash", desc = "Custom Item item hash command")
    @CommandPermissions({"aurora.itemhash"})
    public void hashCmd(CommandSender sender,
                        @Arg(desc = "items", def = "") CustomItemBundle customItems) {
        if (customItems == null) {
            Map<Material, List<CustomItems>> grouped = new HashMap<>();
            for (CustomItems item : CustomItems.values()) {
                List<CustomItems> items = grouped.computeIfAbsent(
                    CustomItemCenter.get(item).getBaseType(),
                    (ignored) -> new ArrayList<>()
                );
                items.add(item);
            }

            for (List<CustomItems> value : grouped.values()) {
                value.sort(Comparator.comparing(CustomItems::getModelId));
            }

            List<Map.Entry<Material, List<CustomItems>>> sortedByKey = Lists.newArrayList(grouped.entrySet());
            sortedByKey.sort(Comparator.comparing((e) -> e.getKey().name()));

            for (Map.Entry<Material, List<CustomItems>> materialListEntry : sortedByKey) {
                sender.sendMessage(materialListEntry.getKey().toString());

                for (CustomItems item : materialListEntry.getValue()) {
                    sender.sendMessage(Text.of(
                        " - ", item.getColoredName(),
                        ChatColor.YELLOW, ": ", ChatColor.BLUE, item.getModelId()
                    ).build());
                }
            }
            return;
        }

        // Map all to verify via exceptions first
        List<CustomItems> items = customItems.getItems();

        // Print hashes of items
        for (CustomItems item : items) {
            sender.sendMessage(Text.of(
                item.getColoredName(),
                ChatColor.YELLOW, ": ", ChatColor.BLUE, item.getModelId()
            ).build());
        }
    }
}
