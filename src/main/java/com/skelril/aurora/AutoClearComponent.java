package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.skelril.aurora.events.DropClearPulseEvent;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Auto Clear", desc = "Automatically clears items on the ground.")
public class AutoClearComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;
    private HashMap<World, Integer> activeWorlds = new HashMap<>();
    private HashMap<World, Integer> worldTimer = new HashMap<>();
    private List<World> recentList = new ArrayList<>();

    @Override
    public void enable() {

        this.config = configure(new LocalConfiguration());
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("min-item-count")
        public int itemCountMin = 1000;
        @Setting("max-delay")
        public int maxDelay = 120;
    }

    @Override
    public void run() {

        for (World world : server.getWorlds()) {

            if (recentList.contains(world)) continue;

            int itemCount = world.getEntitiesByClasses(Item.class, Arrow.class, ExperienceOrb.class).size();

            // Don't spam
            if (itemCount >= (config.itemCountMin * 3) && !activeWorlds.containsKey(world)) {
                dropClear(world, 0);
            } else if ((itemCount >= config.itemCountMin) && !activeWorlds.containsKey(world)) {
                dropClear(world, 10);
            }
        }
    }

    public class Commands {

        @Command(aliases = {"dropclear", "dc"},
                usage = "[seconds] [world] or <world> [seconds]", desc = "Clear all drops",
                min = 0, max = 2)
        @CommandPermissions({"aurora.dropclear"})
        public void dropClearCmd(CommandContext args, CommandSender sender) {

            String secondsString = "10";
            World world = null;

            // Important Check
            if ((sender instanceof Player)) {
                String senderName = sender.getName();
                Player player = Bukkit.getPlayerExact(senderName);
                world = player.getWorld();
                if (args.argsLength() == 0) {
                    world = player.getWorld();
                } else if (args.argsLength() == 1) {
                    secondsString = args.getString(0);
                } else {
                    world = Bukkit.getWorld(args.getString(1));
                    secondsString = args.getString(0);
                }
            } else {
                if (args.argsLength() == 0) {
                    ChatUtil.sendError(sender, "You are not a player and must specify a world!");
                } else if (args.argsLength() == 1) {
                    world = Bukkit.getWorld(args.getString(0));
                } else {
                    world = Bukkit.getWorld(args.getString(0));
                    secondsString = args.getString(1);
                }
            }

            int seconds = Integer.parseInt(secondsString);

            // Don't send a fake world
            if (Bukkit.getWorlds().contains(world)) {

                if (seconds > config.maxDelay) {
                    dropClear(world, config.maxDelay);
                } else {
                    dropClear(world, seconds);
                }
            } else {
                ChatUtil.sendError(sender, "Invalid world name.");
            }
        }
    }

    private void dropClear(final World world, final int seconds) {

        int delay = 0;
        if (seconds > 0) {
            Bukkit.broadcastMessage(ChatColor.RED + "Clearing all " +
                    world.getName() + " drops in " + seconds + " seconds!");
            delay = 20;
        }
        worldTimer.put(world, seconds);

        // New Task
        int taskId = inst.getServer().getScheduler().scheduleSyncRepeatingTask(inst, new Runnable() {

            @Override
            public void run() {

                final Collection<Entity> entityCollection = world.getEntitiesByClasses(Item.class, Arrow.class,
                        ExperienceOrb.class);
                final int clearCount = entityCollection.size();

                int timerSeconds = 0;
                if (worldTimer.containsKey(world)) {
                    timerSeconds = worldTimer.get(world) - 1;
                    worldTimer.put(world, timerSeconds);

                    DropClearPulseEvent event = new DropClearPulseEvent(world, timerSeconds);
                    server.getPluginManager().callEvent(event);
                    timerSeconds = event.getSecondsLeft();
                }


                boolean force = clearCount > config.itemCountMin * 3;
                if ((timerSeconds > 0 && timerSeconds % 5 == 0 || timerSeconds <= 10 && timerSeconds > 0) && !force) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Clearing all " + world.getName() + " drops in "
                            + timerSeconds + " seconds!");
                } else if (timerSeconds < 1 || force) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Clearing all " + world.getName() + " drops!");

                    // Remove Entities
                    for (Entity ent : entityCollection) {

                        if (ent.isValid()) ent.remove();
                    }

                    Bukkit.broadcastMessage(ChatColor.GREEN + "" + clearCount + " drops cleared!");

                    // Add to recent List
                    recentList.add(world);
                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            recentList.remove(world);
                        }
                    }, 20 * 3);

                    // Shut down
                    if (activeWorlds.containsKey(world)) {
                        server.getScheduler().cancelTask(activeWorlds.get(world));
                        activeWorlds.remove(world);
                    }
                    if (worldTimer.containsKey(world)) worldTimer.remove(world);
                }
            }
        }, delay, 20); // Multiply seconds by 20 to convert to ticks
        if (activeWorlds.containsKey(world)) {
            server.getScheduler().cancelTask(activeWorlds.get(world));
        }
        activeWorlds.put(world, taskId);
    }
}