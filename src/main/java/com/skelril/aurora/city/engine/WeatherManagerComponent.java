package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Weather Manager", desc = "Turn off the storm!")
public class WeatherManagerComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        registerCommands(Commands.class);
    }

    public class Commands {

        @Command(aliases = {"stopweather"},
                usage = "", desc = "Hide all storms",
                flags = "r", min = 0, max = 0)
        public void showStormCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            if (args.hasFlag('r')) {
                ((Player) sender).resetPlayerWeather();
                ChatUtil.sendNotice(sender, "Storms are no longer hidden.");
            } else {
                ((Player) sender).setPlayerWeather(WeatherType.CLEAR);
                ChatUtil.sendNotice(sender, "Storms are now hidden.");
                ChatUtil.sendNotice(sender, "To show storms again please use /stopweather -r.");
            }
        }
    }
}
