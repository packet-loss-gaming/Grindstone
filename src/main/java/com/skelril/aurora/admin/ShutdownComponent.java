package com.skelril.aurora.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.skelril.aurora.JungleRaidComponent;
import com.skelril.aurora.events.ServerShutdownEvent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Shudown", desc = "Shutdown system")
public class ShutdownComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    JungleRaidComponent jungleRaid;

    @Override
    public void enable() {

        registerCommands(Commands.class);
    }

    public class Commands {

        @Command(aliases = {"shutdown"}, usage = "[time] [excepted downtime]",
                desc = "Used to restart the server", min = 0, max = 2)
        @CommandPermissions({"aurora.admin.server.shutdown"})
        public void shutdownCmd(CommandContext args, CommandSender sender) throws CommandException {

            int delay = 60;
            long expectedDowntime = TimeUnit.SECONDS.toMillis(30);
            if (args.argsLength() > 0) {
                try {
                    delay = Math.min(120, Math.max(10, Integer.parseInt(args.getString(0))));
                    if (args.argsLength() > 1) {
                        expectedDowntime = CommandBookUtil.matchDate(args.getString(1));
                    }
                } catch (NumberFormatException ex) {
                    throw new CommandException("Invalid time entered!");
                }
            }
            shutdown(delay, expectedDowntime);
        }
    }

    private int seconds = 0;
    private long downTime = 0;
    private BukkitTask task = null;

    public void shutdown(final int assignedSeconds, long expectedDownTime) {

        this.downTime = expectedDownTime;

        if (assignedSeconds > 0) {
            this.seconds = assignedSeconds;
        } else {
            server.shutdown();
            return;
        }

        if (task != null) return;

        final NumberFormat formatter = new DecimalFormat("#");

        // New Task
        task = inst.getServer().getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                server.getPluginManager().callEvent(new ServerShutdownEvent(seconds));
                if (seconds > 0 && seconds % 5 == 0 || seconds <= 10 && seconds > 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Shutting down in " + seconds + " seconds - for "
                            + formatter.format(downTime / 1000) + " seconds of downtime!");
                } else if (seconds < 1) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Shutting down!");
                    server.shutdown();
                    return;
                }
                seconds -= 1;
            }
        }, 0, 20); // Multiply seconds by 20 to convert to ticks
    }
}
