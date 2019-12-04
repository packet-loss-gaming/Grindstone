package gg.packetloss.grindstone.betterweather;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.probability.WeightedPicker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Better Weather", desc = "Improves weather mechanics.")
public class BetterWeatherComponent extends BukkitComponent implements Runnable, Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private WeatherState weatherState = new WeatherState();

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        registerCommands(Commands.class);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 20 * 60);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("forecast.num-to-create")
        public int numToCreate = 20;
        @Setting("forecast.num-to-print")
        public int numToPrint = 5;
        @Setting("duration.shortest")
        public int shortestEvent = 5;
        @Setting("duration.longest")
        public int longestEvent = 30;
        @Setting("thunder-warning-duration.shortest")
        public int shortestThunderWarning = 2;
        @Setting("thunder-warning-duration.longest")
        public int longestThunderWarning = 5;
        @Setting("affected-worlds")
        public List<String> affectedWorlds = List.of("City", "Wilderness", "Legit");
        @Setting("storm-type-weights.clear")
        public int clearStormTypeWeight = 4;
        @Setting("storm-type-weights.rain")
        public int rainStormTypeWeight = 1;
        @Setting("storm-type-weights.thunder-storm")
        public int thunderStormStormTypeWeight = 1;
    }

    private void syncWeather(World world, WeatherType weatherType) {
        // Update weather durations
        int duration = 20 * 60 * (config.longestEvent + 1);
        world.setWeatherDuration(duration);
        world.setThunderDuration(duration);

        // Update weather type
        world.setStorm(weatherType.isStorm());
        world.setThundering(weatherType.hasThunder());
    }

    private void changeWeather() {
        Optional<WeatherEvent> optNewEvent = weatherState.getNewWeatherEvent();
        if (optNewEvent.isEmpty()) {
            return;
        }

        WeatherEvent event = optNewEvent.get();
        for (String worldName : config.affectedWorlds) {
            World affectedWorld = Bukkit.getWorld(worldName);
            WeatherType weatherType = event.getWeatherType();

            syncWeather(affectedWorld, weatherType);
        }
    }

    private WeatherType pickWeather() {
        WeightedPicker<WeatherType> weatherTypes = new WeightedPicker<>();

        weatherTypes.add(WeatherType.CLEAR, config.clearStormTypeWeight);
        weatherTypes.add(WeatherType.RAIN, config.rainStormTypeWeight);
        weatherTypes.add(WeatherType.THUNDERSTORM, config.thunderStormStormTypeWeight);

        return weatherTypes.pick();
    }

    private void populateWeatherQueue() {
        long nextWeatherEvent = weatherState.getLastWeatherEvent();

        Deque<WeatherEvent> weatherQueue = weatherState.getQueue();
        while (weatherQueue.size() < config.numToCreate) {
            long offset = TimeUnit.MINUTES.toMillis(ChanceUtil.getRangedRandom(config.shortestEvent, config.longestEvent));
            nextWeatherEvent += offset;

            WeatherType newWeather = pickWeather();

            // Queue a bit of rain as a warning about the impending thunderstorm
            if (newWeather == WeatherType.THUNDERSTORM) {
                weatherQueue.add(new WeatherEvent(nextWeatherEvent, WeatherType.RAIN));

                nextWeatherEvent += TimeUnit.MINUTES.toMillis(ChanceUtil.getRangedRandom(
                        config.shortestThunderWarning, config.longestThunderWarning
                ));
            }

            weatherQueue.add(new WeatherEvent(nextWeatherEvent, newWeather));
        }
    }

    private void sendWeatherPrintout(CommandSender sender, Collection<WeatherEvent> events, boolean verbose) {
        WeatherType lastWeatherType = null;
        int printed = 0;

        for (WeatherEvent event : events) {
            WeatherType weatherType = event.getWeatherType();

            if (!verbose) {
                if (printed == config.numToPrint) {
                    break;
                }

                if (printed == 0 && sender instanceof Player) {
                    World world = ((Player) sender).getWorld();

                    boolean stormyStateMatches = weatherType.isStorm() == world.hasStorm();
                    boolean thunderStateMatches = weatherType.hasThunder() == world.isThundering();
                    if (stormyStateMatches && thunderStateMatches) {
                        continue;
                    }
                }

                if (lastWeatherType == weatherType) {
                    continue;
                }
            }

            long activationTime = event.getActivationTime();
            String weatherTypeName = weatherType.name();

            String prefix = verbose ? printed + 1 + "). " : " - ";
            ChatUtil.sendNotice(
                    sender,
                    prefix + TimeUtil.getPrettyTime(activationTime) + " " + ChatColor.BLUE + weatherTypeName
            );

            lastWeatherType = weatherType;
            ++printed;
        }
    }

    private Optional<WeatherType> getWeatherFromToken(String charStr) {
        switch (charStr.charAt(0)) {
            case 'c':
                return Optional.of(WeatherType.CLEAR);
            case 'r':
                return Optional.of(WeatherType.RAIN);
            case 't':
                return Optional.of(WeatherType.THUNDERSTORM);
            default:
                return Optional.empty();
        }
    }

    private Optional<Integer> getRepeatsFromToken(String charStr) {
        if (charStr.length() == 1) {
            return Optional.of(1);
        }

        try {
            return Optional.of(Integer.parseInt(charStr.substring(1)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void run() {
        changeWeather();
        populateWeatherQueue();
    }

    @EventHandler(ignoreCancelled = true)
    public void onStormChange(WeatherChangeEvent event) {
        World world = event.getWorld();

        if (!event.toWeatherState() && world.isThundering()) {
            world.setThundering(false);
        }

        if (config.affectedWorlds.contains(world.getName())) {
            Optional<WeatherType> optCurrentWeather = weatherState.getCurrentWeather();
            if (optCurrentWeather.isEmpty()) {
                return;
            }

            WeatherType weatherType = optCurrentWeather.get();

            switch (weatherType) {
                case THUNDERSTORM:
                case RAIN:
                    if (!event.toWeatherState()) {
                        event.setCancelled(true);
                    }
                    break;
                case CLEAR:
                    if (event.toWeatherState()) {
                        event.setCancelled(true);
                    }
                    break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {
        World world = event.getWorld();
        if (event.toThunderState() && world.getWeatherDuration() < world.getThunderDuration()) {
            world.setWeatherDuration(world.getThunderDuration());
        }

        if (config.affectedWorlds.contains(world.getName())) {
            Optional<WeatherType> optCurrentWeather = weatherState.getCurrentWeather();
            if (optCurrentWeather.isEmpty()) {
                return;
            }

            WeatherType weatherType = optCurrentWeather.get();

            switch (weatherType) {
                case THUNDERSTORM:
                    if (!event.toThunderState()) {
                        event.setCancelled(true);
                    }
                    break;
                case RAIN:
                case CLEAR:
                    if (event.toThunderState()) {
                        event.setCancelled(true);
                    }
                    break;
            }
        }
    }

    public class Commands {
        @Command(aliases = {"forecast"},
                usage = "", desc = "Get the forecast for the server's weather",
                flags = "v", min = 0, max = 0)
        public void forecastCmd(CommandContext args, CommandSender sender) throws CommandException {
            boolean verbose = args.hasFlag('v') && sender.hasPermission("aurora.weather.recast");
            sendWeatherPrintout(sender, weatherState.getQueue(), verbose);
        }

        @Command(aliases = {"forcecast"},
                usage = "<weather type stream>", desc = "Redo the server forecast",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.weather.recast")
        public void forcecastCmd(CommandContext args, CommandSender sender) throws CommandException {
            String newForecastString = args.getString(0);
            String[] forecastElements = newForecastString.split(",");

            Deque<WeatherType> weatherTypes = new LinkedList<>();
            for (String el : forecastElements) {
                Optional<WeatherType> optWeatherType = getWeatherFromToken(el);
                if (optWeatherType.isEmpty()) {
                    throw new CommandException("Unknown weather token: " + el);
                }
                Optional<Integer> repeats = getRepeatsFromToken(el);
                if (repeats.isEmpty()) {
                    throw new CommandException("Non-number formatted repeat: " + el);
                }

                for (int i = repeats.get(); i > 0; --i) {
                    weatherTypes.add(optWeatherType.get());
                }
            }

            WeatherType initialChange = weatherTypes.poll();
            Deque<WeatherEvent> weatherQueue = weatherState.getQueue();
            for (WeatherEvent event : weatherQueue) {
                if (weatherTypes.isEmpty()) {
                    break;
                }
                event.setWeatherType(weatherTypes.poll());
            }
            weatherQueue.addFirst(new WeatherEvent(System.currentTimeMillis(), initialChange));

            changeWeather();
            populateWeatherQueue();

            ChatUtil.sendNotice(sender, "Current weather even changed!");
        }

        @Command(aliases = {"recast"},
                usage = "", desc = "Redo the server forecast",
                flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.weather.recast")
        public void recastCmd(CommandContext args, CommandSender sender) throws CommandException {
            weatherState.getQueue().clear();
            populateWeatherQueue();

            ChatUtil.sendNotice(sender, "Forecast updated!");
        }
    }
}
