/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.MirageArena;

import com.sk89q.commandbook.command.argument.MultiPlayerTarget;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class MirageArenaCommands {
    private MirageArena component;

    public MirageArenaCommands(MirageArena component) {
        this.component = component;
    }

    @Command(name = "vote", desc = "vote for the next arena")
    @CommandPermissions("aurora.mirage.vote")
    public void voteCmd(Player player, @Arg(desc = "arena name") MirageArenaSchematic arena) {
        component.registerVote(player, arena);

        ChatUtil.sendNotice(player, "Your vote has been set to " + arena.getArenaName() + '.');
    }

    @Command(name = "list", desc = "list arenas")
    @CommandPermissions("aurora.mirage.list")
    public void listCmd(CommandSender sender, @ArgFlag(name = 'p', desc = "page", def = "1") int page,
                        @Arg(desc = "name filter text", def = "") String filter) {
        List<MirageArenaSchematic> arenas = component.getArenas(filter);
        arenas.sort(Comparator.comparing(MirageArenaSchematic::getArenaName));

        new TextComponentChatPaginator<MirageArenaSchematic>(ChatColor.GOLD, "Arenas") {
            @Override
            public Optional<String> getPagerCommand(int page) {
                return Optional.of("/mirage list -p " + page + " " + Optional.ofNullable(filter).orElse(""));
            }

            @Override
            public Text format(MirageArenaSchematic arena) {
                return Text.of(
                        ChatColor.BLUE,
                        arena.getArenaName(),
                        TextAction.Click.runCommand("/mirage vote " + arena.getArenaName()),
                        TextAction.Hover.showText(Text.of("Vote for this arena next"))
                );
            }
        }.display(sender, arenas, page);
    }

    @Command(name = "ignore", desc = "Ignore a player")
    @CommandPermissions("aurora.mirage.ignore")
    public void ignoreCmd(Player player, @Arg(desc = "players to ignore") MultiPlayerTarget players) {
        for (Player target : players) {
            component.registerIgnore(player, target);
            ChatUtil.sendNotice(player, "You will no longer be able to damage " + target.getName() + ".");
        }
    }

    @Command(name = "unignore", desc = "Unignore a player")
    @CommandPermissions("aurora.mirage.ignore")
    public void unignoreCmd(Player player, @Arg(desc = "players to unignore") MultiPlayerTarget players) {
        for (Player target : players) {
            component.unregisterIgnore(player, target);
            ChatUtil.sendNotice(player, "You will now be able to damage " + target.getName() + ".");
        }
    }

    @Command(name = "save", desc = "Save an arena state")
    @CommandPermissions("aurora.mirage.save")
    public void saveCmd(CommandSender sender, @Switch(name = 'o', desc = "overwrite existing") boolean overwrite,
                        @Arg(desc = "arena name") String arenaName) throws CommandException {
        component.saveArena(arenaName, overwrite);
        ChatUtil.sendNotice(sender, "Successfully saved.");
    }

    @Command(name = "load", desc = "Load an arena state")
    @CommandPermissions("aurora.mirage.load")
    public void loadCmd(CommandSender sender, @Arg(desc = "arena name") MirageArenaSchematic arena) throws CommandException {
        try {
            component.changeMirage(arena);
        } catch (IOException e) {
            e.printStackTrace();
            ChatUtil.sendError(sender, "Error encountered, check console.");
        }
    }

}
