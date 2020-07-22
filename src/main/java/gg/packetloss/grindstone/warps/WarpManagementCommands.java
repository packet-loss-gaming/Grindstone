/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static gg.packetloss.grindstone.items.custom.CustomItems.TOME_OF_THE_RIFT_SPLITTER;
import static gg.packetloss.grindstone.util.StringUtil.toTitleCase;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class WarpManagementCommands {
    private WarpsComponent component;

    public WarpManagementCommands(WarpsComponent component) {
        this.component = component;
    }

    @Command(name = "set", desc = "Create/update a new warp")
    public void setWarp(Player player,
                        @Switch(name = 'g', desc = "global") boolean global,
                        @Arg(desc = "warp name") String warpName) throws CommandException {
        WarpQualifiedName qualifiedWarpName;
        if (global) {
            CommandBook.inst().checkPermission(player, "aurora.warp.set.global");

            qualifiedWarpName = new WarpQualifiedName(warpName);
        } else {
            CommandBook.inst().checkPermission(player, "aurora.warp.set.self");

            if (!ItemUtil.removeItemOfName(player, CustomItemCenter.build(TOME_OF_THE_RIFT_SPLITTER), 1, false)) {
                throw new CommandException("You need a Tome of the Rift Splitter to add or update a warp.");
            }

            qualifiedWarpName = new WarpQualifiedName(player.getUniqueId(), warpName);
        }

        boolean isUpdate = component.getWarpManager().setWarp(qualifiedWarpName, player.getLocation()).isPresent();
        if (isUpdate) {
            ChatUtil.sendNotice(player, "Warp '" + qualifiedWarpName.getDisplayName() + "' updated.");
        } else {
            ChatUtil.sendNotice(player, "Warp '" + qualifiedWarpName.getDisplayName() + "' created.");
        }
    }

    @Command(name = "destroy", aliases = {"delete", "remove", "del", "des", "rem"}, desc = "Remove a warp")
    public void destroyWarp(Player player,
                        @Switch(name = 'g', desc = "global") boolean global,
                        @Arg(desc = "warp name") String warpName) throws CommandException {
        WarpQualifiedName qualifiedWarpName;
        if (global) {
            CommandBook.inst().checkPermission(player, "aurora.warp.destroy.global");

            qualifiedWarpName = new WarpQualifiedName(warpName);
        } else {
            CommandBook.inst().checkPermission(player, "aurora.warp.destroy.self");

            qualifiedWarpName = new WarpQualifiedName(player.getUniqueId(), warpName);
        }

        if (component.getWarpManager().destroyWarp(qualifiedWarpName)) {
            ChatUtil.sendNotice(player, "Warp '" + qualifiedWarpName.getDisplayName() + "' destroyed.");
        } else {
            ChatUtil.sendNotice(player, "Warp '" + qualifiedWarpName.getDisplayName() + "' not found.");
        }
    }

    private TextComponentChatPaginator<WarpPoint> getListResult() {
        return new TextComponentChatPaginator<WarpPoint>(ChatColor.GOLD, "Warps") {
            @Override
            public Optional<String> getPagerCommand(int page) {
                return Optional.of("/warps list -p " + page);
            }

            @Override
            public Text format(WarpPoint entry) {
                WarpQualifiedName qualifiedName = entry.getQualifiedName();

                String titleCaseName = toTitleCase(entry.getQualifiedName().getName());
                Text warpName = Text.of(
                        (qualifiedName.isGlobal() ? ChatColor.BLUE : ChatColor.DARK_BLUE),
                        qualifiedName.getDisplayName().toUpperCase(),
                        TextAction.Click.runCommand("/warp " + entry.getQualifiedName()),
                        TextAction.Hover.showText(Text.of("Teleport to ", titleCaseName))
                );

                return Text.of(warpName, ChatColor.YELLOW, " (World: ", Text.of(entry.getWorldName()), ")");
            }
        };
    }

    @Command(name = "list", desc = "List warps")
    @CommandPermissions({"aurora.warp.list"})
    public void listCmd(CommandSender sender,
                        @ArgFlag(name = 'p', desc = "Page of results to return", def = "1") int page,
                        @Arg(desc = "String to filter on", def = "") String filter) throws CommandException {
        List<WarpPoint> warps = component.getWarpManager().getWarpsForPlayer(PlayerUtil.checkPlayer(sender));

        // Filter out unwanted warps
        if (filter != null) {
            warps.removeIf(warp -> !warp.getQualifiedName().getDisplayName().startsWith(filter.toUpperCase()));
        }

        // Sort warps for display
        warps.sort(Comparator.comparing(p -> p.getQualifiedName().isGlobal()));
        warps.sort(Comparator.comparing(p -> p.getQualifiedName().getDisplayName()));

        getListResult().display(sender, warps, page);
    }
}
