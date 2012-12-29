package us.arrowcraft.aurora.easycommandsystem;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.ChatUtil;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Home Manager", desc = "Home ECS")
@Depend(plugins = {"WorldEdit", "WorldGuard"})
public class HomeManagerComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        registerCommands(Commands.class);
    }

    public class Commands {

        @Command(aliases = {"home", "he"}, desc = "Home Manager")
        @NestedCommand({NestedCommands.class})
        public void homeCmd(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"addplayer"}, usage = "<player>", desc = "Add a player to your home",
                min = 1, max = 1)
        @CommandPermissions({"aurora.home.self.add"})
        public void addMemberToHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerToAdd = args.getString(0);

                Bukkit.dispatchCommand(sender, "rg addmember " + getHome(player) + " " + playerToAdd);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"removeplayer"}, usage = "<player>", desc = "Remove a player from your home",
                min = 1, max = 1)
        @CommandPermissions({"aurora.home.self.add"})
        public void removeMemberToHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerToRemove = args.getString(0);

                Bukkit.dispatchCommand(sender, "rg removemember " + getHome(player) + " " + playerToRemove);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"flag"}, usage = "<flag>", desc = "Flag a home",
                min = 1)
        @CommandPermissions({"aurora.home.self.flag"})
        public void flagHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;
                String flag = args.getJoinedStrings(0);

                Bukkit.dispatchCommand(sender, "rg flag " + getHome(player) + " " + flag);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"location", "loc"}, desc = "Home Manager")
        @NestedCommand({TeleportCommands.class})
        public void homeTeleportCmds(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"admin"}, desc = "Admin Commands")
        @NestedCommand({HomeAdminCommands.class})
        public void homeAdminCmd(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class HomeAdminCommands {

        @Command(aliases = {"create", "add"}, usage = "<player> <district>", desc = "Create a home",
                min = 2, max = 2)
        @CommandPermissions({"aurora.home.admin.create"})
        public void createHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String player = args.getString(0);
                String district = args.getString(1);

                Bukkit.dispatchCommand(sender, "rg define " + getHome(player));
                Bukkit.dispatchCommand(sender, "rg addowner " + getHome(player) + " " + player);
                Bukkit.dispatchCommand(sender, "rg setpriority " + getHome(player) + " 10");
                Bukkit.dispatchCommand(sender, "rg setparent " + getHome(player) + " " + district + "-district");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"flag"}, usage = "<player> <flag>", desc = "Flag a home",
                min = 2)
        @CommandPermissions({"aurora.home.admin.flag"})
        public void flagHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String player = args.getString(0);
                String flag = args.getJoinedStrings(1);

                Bukkit.dispatchCommand(sender, "rg flag " + getHome(player) + " " + flag);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"biome"}, usage = "<player> <newbiome>", desc = "Change the biome of a home",
                min = 2, max = 2)
        @CommandPermissions({"aurora.home.admin.biome.change"})
        public void homeBiomeChangeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String player = args.getString(0);
                String biomeString = args.getString(1);

                Bukkit.dispatchCommand(sender, "rg select " + getHome(player));
                Bukkit.dispatchCommand(sender, "/setbiome " + biomeString);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"move"}, usage = "<player> <newdistrict>", desc = "Move a home",
                min = 2, max = 2)
        @CommandPermissions({"aurora.home.admin.create"})
        public void moveHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String player = args.getString(0);
                String newDistrict = args.getString(1);

                Bukkit.dispatchCommand(sender, "rg redefine " + getHome(player));
                Bukkit.dispatchCommand(sender, "rg setparent " + getHome(player) + " " + newDistrict + "-district");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"remove"}, usage = "<player>", desc = "Remove a home", min = 1, max = 1)
        @CommandPermissions({"aurora.home.admin.remove"})
        public void removeHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String player = args.getString(0);

                Bukkit.dispatchCommand(sender, "rg remove " + getHome(player));
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }
    }

    private String getHome(String player) {

        return player + "'s-house";
    }

    private String getHome(Player player) {

        return player.getName() + "'s-house";
    }

    public class TeleportCommands {

        @Command(aliases = {"set"}, desc = "Set your home")
        public void setHomeLoc(CommandContext args, CommandSender sender) {

            ChatUtil.sendNotice(sender, "To set your home, sleep in a bed during the night.");
        }

        @Command(aliases = {"teleport", "tp"}, desc = "Go to your home")
        public void teleportHome(CommandContext args, CommandSender sender) {

            ChatUtil.sendNotice(sender, "To go to your home, throw an ender pearl at your feet.");
        }

    }
}
