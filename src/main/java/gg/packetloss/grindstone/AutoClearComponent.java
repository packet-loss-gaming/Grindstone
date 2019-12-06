/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.grindstone.util.timer.TimerUtil;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Auto Clear", desc = "Automatically clears items on the ground.")
@Depend(components = {SessionComponent.class})
public class AutoClearComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;

    private LocalConfiguration config;
    private HashMap<World, Integer> worldCount = new HashMap<>();
    private HashMap<World, TimedRunnable> worldTimer = new HashMap<>();
    private Map<World, Collection<ChunkStats>> lastClear = new HashMap<>();

    @Override
    public void enable() {
        this.config = configure(new LocalConfiguration());
        registerCommands(Commands.class);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
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

    private static Set<EntityType> checkedEntities = new HashSet<>();

    static {
        checkedEntities.add(EntityType.DROPPED_ITEM);
        checkedEntities.add(EntityType.ARROW);
        checkedEntities.add(EntityType.EXPERIENCE_ORB);
    }

    private BukkitTask nextTickCleanupTask = null;

    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (!checkedEntities.contains(entity.getType())) {
            return;
        }

        World world = event.getEntity().getWorld();
        worldCount.compute(world, (ignored, existingCount) -> {
            if (existingCount == null) {
                existingCount = 0;
            }

            return ++existingCount;
        });

        int computedCount = worldCount.getOrDefault(world, 0);
        if (computedCount >= (config.itemCountMin * 3)) {
            if (nextTickCleanupTask == null) {
                nextTickCleanupTask = server.getScheduler().runTask(inst, () -> {
                    emergencyDropClear(world);
                    nextTickCleanupTask = null;
                });
            }
        } else if (computedCount > config.itemCountMin) {
            dropClear(world, 10, false);
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (!checkedEntities.contains(entity.getType())) {
            return;
        }

        World world = event.getEntity().getWorld();
        worldCount.compute(world, (ignored, existingCount) -> {
            if (existingCount == null) {
                existingCount = 1;
            }

            return Math.max(0, --existingCount);
        });
    }

    private CheckProfile checkEntities(World world) {
        Set<Entity> entities = new HashSet<>();
        Set<ChunkStats> stats = new HashSet<>();
        Chunk[] loaded = world.getLoadedChunks();
        for (Chunk chunk : loaded) {
            ChunkStats cs = new ChunkStats(chunk);
            for (Entity e : chunk.getEntities()) {
                checkedEntities.stream().filter(eType -> eType == e.getType()).forEach(eType -> {
                    cs.increase(eType, 1);
                    entities.add(e);
                });
            }
            if (cs.total() < 1) continue;
            stats.add(cs);
        }
        return new CheckProfile(entities, stats);
    }

    public class Commands {

        @Command(aliases = {"dropclear", "dc"},
                usage = "[seconds] [world] or <world> [seconds]", desc = "Clear all drops",
                min = 0, max = 2)
        @CommandPermissions({"aurora.dropclear"})
        public void dropClearCmd(CommandContext args, CommandSender sender) throws CommandException {

            World world;
            int seconds = 10;

            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
                if (args.argsLength() > 1) {
                    world = Bukkit.getWorld(args.getString(1));
                    seconds = args.getInteger(0);
                } else if (args.argsLength() == 1) {
                    seconds = args.getInteger(0);
                }
            } else {
                if (args.argsLength() == 0) {
                    throw new CommandException("You are not a player and must specify a world!");
                } else if (args.argsLength() == 1) {
                    world = Bukkit.getWorld(args.getString(0));
                } else {
                    world = Bukkit.getWorld(args.getString(0));
                    seconds = args.getInteger(1);
                }
            }

            if (world == null) {
                throw new CommandException("No world by that name found!");
            }
            dropClear(world, Math.max(0, Math.min(seconds, config.maxDelay)), true);
        }

        @Command(aliases = {"dropstats", "ds"}, desc = "Drop statistics")
        @NestedCommand(DropStatsCommands.class)
        @CommandPermissions({"aurora.dropclear.stats"})
        public void dropStatsCmds(CommandContext args, CommandSender sender) {

        }
    }

    public class DropStatsCommands {
        @Command(aliases = {"tallies"},
                usage = "", desc = "List world tallies",
                min = 0, max = 0)
        public void talliesCmd(CommandContext args, CommandSender sender) throws CommandException {
            worldCount.forEach((world, count) -> {
                ChatUtil.sendNotice(sender, world.getName() + ": " + count);
            });
        }

        @Command(aliases = {"update"},
                usage = "<world>", desc = "Updates your copy of stats for that world",
                min = 1, max = 1)
        public void updateCmd(CommandContext args, CommandSender sender) throws CommandException {
            World world = Bukkit.getWorld(args.getString(0));
            if (world == null) {
                throw new CommandException("No world by that name found!");
            }

            sessions.getSession(DropClearObserver.class, sender).setStats(world, checkEntities(world).getStats());
            ChatUtil.sendNotice(sender, "Stats updated.");
        }

        @Command(aliases = {"info"},
                usage = "<world> <drop|last> <x> <z>", desc = "View details of a certain chunk",
                min = 4, max = 4)
        public void infoCmd(CommandContext args, CommandSender sender) throws CommandException {
            World world = Bukkit.getWorld(args.getString(0));
            if (world == null) {
                throw new CommandException("No world by that name found!");
            }

            Collection<ChunkStats> stats;
            String typeString = args.getString(1);
            if (typeString.equalsIgnoreCase("drop")) {
                stats = lastClear.get(world);
                if (stats == null) {
                    throw new CommandException("No recent drop clears on record.");
                }
            } else if (typeString.equalsIgnoreCase("last")) {
                stats = sessions.getSession(DropClearObserver.class, sender).getStats(world);
                if (stats == null) {
                    throw new CommandException("No snapshots on record.");
                }
            } else {
                throw new CommandException("Unsupported stats specified!");
            }

            int x = args.getInteger(2);
            int z = args.getInteger(3);

            ChunkStats target = null;
            for (ChunkStats cs : stats) {
                if (cs.getX() == x && cs.getZ() == z) {
                    target = cs;
                    break;
                }
            }

            if (target == null) {
                throw new CommandException("That chunk could not be found in the specified record");
            }

            ChatUtil.sendNotice(sender, ChatColor.GOLD, "Chunk stats: (X: " + ChatColor.WHITE + target.getX()
                    + "%p%, Z: " + ChatColor.WHITE + target.getZ()
                    + "%p%)");
            ChatUtil.sendNotice(sender, "Total Drops: " + target.total());
            for (Map.Entry<EntityType, Integer> entry : target.getStats().entrySet()) {
                ChatUtil.sendNotice(sender, entry.getKey().name() + ": " + entry.getValue());
            }
        }

        @Command(aliases = {"chunks"},
                usage = "<world> <drop|last>", desc = "View chunk stats",
                flags = "p:", min = 2, max = 2)
        public void chunksCmd(CommandContext args, CommandSender sender) throws CommandException {
            World world = Bukkit.getWorld(args.getString(0));
            if (world == null) {
                throw new CommandException("No world by that name found!");
            }

            Collection<ChunkStats> stats;
            String typeString = args.getString(1);
            if (typeString.equalsIgnoreCase("drop")) {
                stats = lastClear.get(world);
                if (stats == null) {
                    throw new CommandException("No recent drop clears on record.");
                }
            } else if (typeString.equalsIgnoreCase("last")) {
                stats = sessions.getSession(DropClearObserver.class, sender).getStats(world);
                if (stats == null) {
                    throw new CommandException("No snapshots on record.");
                }
            } else {
                throw new CommandException("Unsupported stats specified!");
            }

            List<ChunkStats> statsList = Lists.newArrayList(stats);
            statsList.sort((o1, o2) -> o2.total() - o1.total());

            new PaginatedResult<ChunkStats>(ChatColor.GOLD + "Chunk Stats") {
                @Override
                public String format(ChunkStats chunkStats) {
                    int total = chunkStats.total();
                    ChatColor recordColor = total >= config.itemCountMin / 8 ? ChatColor.RED : ChatColor.BLUE;
                    return recordColor + String.valueOf(total)
                            + ChatColor.YELLOW + " (X: " + ChatColor.WHITE + chunkStats.getX()
                            + ChatColor.YELLOW + ", Z: " + ChatColor.WHITE + chunkStats.getZ()
                            + ChatColor.YELLOW + ')';
                }
            }.display(sender, statsList, args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"composition", "comp"},
                usage = "<world> <drop|last>", desc = "View composition stats",
                min = 2, max = 2)
        public void compositionCmd(CommandContext args, CommandSender sender) throws CommandException {
            World world = Bukkit.getWorld(args.getString(0));
            if (world == null) {
                throw new CommandException("No world by that name found!");
            }

            Collection<ChunkStats> stats;
            String typeString = args.getString(1);
            if (typeString.equalsIgnoreCase("drop")) {
                stats = lastClear.get(world);
                if (stats == null) {
                    throw new CommandException("No recent drop clears on record.");
                }
            } else if (typeString.equalsIgnoreCase("last")) {
                stats = sessions.getSession(DropClearObserver.class, sender).getStats(world);
                if (stats == null) {
                    throw new CommandException("No snapshots on record.");
                }
            } else {
                throw new CommandException("Unsupported stats specified!");
            }

            Map<EntityType, Integer> totals = new HashMap<>();
            for (ChunkStats cs : stats) {
                Map<EntityType, Integer> mapping = cs.getStats();
                for (EntityType type : checkedEntities) {
                    Integer newVal = mapping.get(type);
                    if (newVal == null) continue;
                    Integer curVal = totals.get(type);
                    if (curVal != null) {
                        newVal += curVal;
                    }
                    totals.put(type, newVal);
                }
            }

            int total = 0;
            for (Integer i : totals.values()) {
                total += i;
            }

            ChatUtil.sendNotice(sender, ChatColor.GOLD, "Drop Composition Report");
            DecimalFormat formatter = new DecimalFormat("#.##");
            for (Map.Entry<EntityType, Integer> entry : totals.entrySet()) {
                ChatUtil.sendNotice(sender, ChatColor.YELLOW, entry.getKey().name() + " (Quantity: "
                        + ChatColor.WHITE + entry.getValue()
                        + "%p% - "
                        + ChatColor.WHITE + formatter.format(((double) entry.getValue() / total) * 100)
                        + "%p%%)");
            }
        }
    }

    private void clearWorld(World world) {
        Bukkit.broadcastMessage(ChatColor.RED + "Clearing all " + world.getName() + " drops!");
        CheckProfile profile = checkEntities(world);
        lastClear.put(world, profile.getStats());
        Collection<Entity> entities = profile.getEntities();
        entities.forEach(Entity::remove);
        Bukkit.broadcastMessage(String.valueOf(ChatColor.GREEN) + entities.size() + " drops cleared!");
    }

    private void emergencyDropClear(World world) {
        clearWorld(world);

        TimedRunnable runnable = worldTimer.get(world);
        if (runnable != null && !runnable.isComplete()) {
            runnable.cancel();
        }
    }

    private void dropClear(World world, int seconds, boolean overwrite) {

        TimedRunnable runnable = worldTimer.get(world);

        // Check for old task, and overwrite if allowed
        if (runnable != null && !runnable.isComplete()) {
            if (overwrite) {
                runnable.setTimes(seconds);
            }
            return;
        }

        IntegratedRunnable dropClear = new IntegratedRunnable() {

            @Override
            public boolean run(int times) {
                if (TimerUtil.matchesFilter(times, 10, 5)) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Clearing all "
                            + world.getName() + " drops in "
                            + times + " seconds!");
                }
                return true;
            }

            @Override
            public void end() {
                clearWorld(world);
            }
        };

        // Setup new task
        runnable = new TimedRunnable(dropClear, seconds);
        // Offset this by one to prevent the drop clear from triggering twice
        runnable.setTask(server.getScheduler().runTaskTimer(inst, runnable, 1, 20));
        worldTimer.put(world, runnable);
    }

    private class CheckProfile {
        private final Collection<Entity> entities;
        private final Collection<ChunkStats> stats;

        private CheckProfile(Collection<Entity> entities, Collection<ChunkStats> stats) {
            this.entities = entities;
            this.stats = stats;
        }

        public Collection<Entity> getEntities() {
            return entities;
        }

        public Collection<ChunkStats> getStats() {
            return stats;
        }
    }

    private class ChunkStats {
        private Map<EntityType, Integer> counterQuantity = new HashMap<>();
        private final int x;
        private final int z;

        public ChunkStats(Chunk chunk) {
            x = chunk.getX();
            z = chunk.getZ();
        }

        public void increase(EntityType type, int amt) {
            Integer count = counterQuantity.get(type);
            if (count != null) {
                count += amt;
            } else {
                count = amt;
            }
            counterQuantity.put(type, count);
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }

        public int total() {
            int total = 0;
            for (Integer i : counterQuantity.values()) {
                total += i;
            }
            return total;
        }

        public Map<EntityType, Integer> getStats() {
            return Collections.unmodifiableMap(counterQuantity);
        }
    }

    private static class DropClearObserver extends PersistentSession {

        private Map<World, Collection<ChunkStats>> lastSnapshot = new HashMap<>();

        public DropClearObserver() {
            super(THIRTY_MINUTES);
        }

        public void setStats(World world, Collection<ChunkStats> stats) {
            lastSnapshot.put(world, stats);
        }

        public boolean hasStats(World world) {
            return lastSnapshot.containsKey(world);
        }

        public Collection<ChunkStats> getStats(World world) {
            Collection<ChunkStats> stats = lastSnapshot.get(world);
            return stats == null ? null : Collections.unmodifiableCollection(stats);
        }
    }
}