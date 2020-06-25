package gg.packetloss.grindstone.betterweather;

import com.google.gson.Gson;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.events.BetterWeatherChangeEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.probability.WeightedPicker;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldMassQuery;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.zachsthings.libcomponents.bukkit.BasePlugin.callEvent;

@ComponentInformation(friendlyName = "Better Weather", desc = "Improves weather mechanics.")
@Depend(components = {ManagedWorldComponent.class})
public class BetterWeatherComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private ManagedWorldComponent managedWorld;

    private Path statesDir;

    private Gson gson = new Gson();

    private WeatherState weatherState = new WeatherState();

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        try {
            Path baseDir = Path.of(inst.getDataFolder().getPath(), "state");
            statesDir = Files.createDirectories(baseDir.resolve("states"));

            loadWeatherState();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        registerCommands(Commands.class);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        server.getScheduler().runTask(inst, this::syncWeather);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this::updateWeather, 0, 20 * 60);
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
        @Setting("duration.clear.shortest")
        public int shortestClearEvent = 5;
        @Setting("duration.clear.longest")
        public int longestClearEvent = 30;
        @Setting("duration.rain.shortest")
        public int shortestRainEvent = 5;
        @Setting("duration.rain.longest")
        public int longestRainEvent = 30;
        @Setting("duration.thunder.shortest")
        public int shortestThunderEvent = 5;
        @Setting("duration.thunder.longest")
        public int longestThunderEvent = 30;
        @Setting("thunder-warning-duration.shortest")
        public int shortestThunderWarning = 2;
        @Setting("thunder-warning-duration.longest")
        public int longestThunderWarning = 5;
        @Setting("storm-type-weights.clear")
        public int clearStormTypeWeight = 4;
        @Setting("storm-type-weights.rain")
        public int rainStormTypeWeight = 1;
        @Setting("storm-type-weights.thunder-storm")
        public int thunderStormStormTypeWeight = 1;
    }

    private Path getStateFile() {
        return statesDir.resolve("weather.json");
    }

    private void loadWeatherState() {
        Path stateFile = getStateFile();
        if (!Files.exists(stateFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(stateFile)) {
            weatherState = gson.fromJson(reader, WeatherState.class);
        } catch (IOException e) {
            log.warning("Failed to load previous weather state");
            e.printStackTrace();
        }
    }

    private void saveWeatherState() {
        if (!weatherState.isDirty()) {
            return;
        }

        Path stateFile = getStateFile();
        try (BufferedWriter writer = Files.newBufferedWriter(
                stateFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(gson.toJson(weatherState));
            weatherState.resetDirtyFlag();
        } catch (IOException e) {
            log.warning("Failed to save previous weather state");
            e.printStackTrace();
        }
    }

    private int getMinDuration(WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return config.shortestClearEvent;
            case RAIN:
                return config.shortestRainEvent;
            case THUNDERSTORM:
                return config.shortestThunderEvent;
        }

        throw new UnsupportedOperationException();
    }


    private int getMaxDuration(WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return config.longestClearEvent;
            case RAIN:
                return config.longestRainEvent;
            case THUNDERSTORM:
                return config.longestThunderEvent;
        }

        throw new UnsupportedOperationException();
    }

    private int getMaxDuration() {
        int i = 0;
        for (WeatherType weatherType : WeatherType.values()) {
            i = Math.max(i, getMaxDuration(weatherType));
        }
        return i;
    }

    private void syncWeather(World world, WeatherType weatherType) {
        // Update weather durations
        int duration = 20 * 60 * (getMaxDuration() + 1);
        world.setWeatherDuration(duration);
        world.setThunderDuration(duration);

        // Update weather type
        world.setStorm(weatherType.isStorm());
        world.setThundering(weatherType.hasThunder());
    }

    private void syncWeather(WeatherType oldWeatherType, WeatherType newWeatherType) {
        for (World affectedWorld : managedWorld.getAll(ManagedWorldMassQuery.ENVIRONMENTALLY_CONTROLLED)) {
            if (oldWeatherType != newWeatherType) {
                callEvent(new BetterWeatherChangeEvent(affectedWorld, oldWeatherType, newWeatherType));
            }

            syncWeather(affectedWorld, newWeatherType);
        }
    }

    private void syncWeather() {
        syncWeather(weatherState.getCurrentWeather(), weatherState.getCurrentWeather());
    }

    private void changeWeather() {
        WeatherType oldWeather = weatherState.getCurrentWeather();
        Optional<WeatherEvent> optNewEvent = weatherState.getNewWeatherEvent();
        if (optNewEvent.isEmpty()) {
            return;
        }

        WeatherEvent event = optNewEvent.get();
        syncWeather(oldWeather, event.getWeatherType());
    }

    private WeatherType pickWeather() {
        WeightedPicker<WeatherType> weatherTypes = new WeightedPicker<>();

        weatherTypes.add(WeatherType.CLEAR, config.clearStormTypeWeight);
        weatherTypes.add(WeatherType.RAIN, config.rainStormTypeWeight);
        weatherTypes.add(WeatherType.THUNDERSTORM, config.thunderStormStormTypeWeight);

        return weatherTypes.pick();
    }

    private void populateWeatherQueue() {
        Optional<WeatherEvent> lastWeatherEvent = weatherState.getLastWeatherEventTime();

        WeatherType lastWeatherType = lastWeatherEvent
                .map(WeatherEvent::getWeatherType)
                .orElseGet(() -> weatherState.getCurrentWeather());

        long lastWeatherEventTime = lastWeatherEvent
                .map(WeatherEvent::getActivationTime)
                .orElseGet(System::currentTimeMillis);

        while (weatherState.getQueueSize() < config.numToCreate) {
            // Calculate the new weather event and its time
            WeatherType newWeatherType = pickWeather();

            // We need to calculate the offset based on the last weather event's time as
            // we're setting the end of that event, and the start of this event.
            long offset = TimeUnit.MINUTES.toMillis(ChanceUtil.getRangedRandom(
                    getMinDuration(lastWeatherType),
                    getMaxDuration(lastWeatherType)
            ));
            long newWeatherEventTime = lastWeatherEventTime + offset;

            // Queue a bit of rain as a warning about the impending thunderstorm
            if (newWeatherType == WeatherType.THUNDERSTORM && lastWeatherType == WeatherType.CLEAR) {
                weatherState.addToQueue(new WeatherEvent(newWeatherEventTime, WeatherType.RAIN));

                newWeatherEventTime += TimeUnit.MINUTES.toMillis(ChanceUtil.getRangedRandom(
                        config.shortestThunderWarning, config.longestThunderWarning
                ));
            }

            weatherState.addToQueue(new WeatherEvent(newWeatherEventTime, newWeatherType));

            // Update the last value trackers
            lastWeatherType = newWeatherType;
            lastWeatherEventTime = newWeatherEventTime;
        }
    }

    private void updateWeather() {
        populateWeatherQueue();
        changeWeather();
        saveWeatherState();
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
            return Optional.of(Math.max(1, Integer.parseInt(charStr.substring(1))));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStormChange(WeatherChangeEvent event) {
        World world = event.getWorld();

        if (!event.toWeatherState() && world.isThundering()) {
            world.setThundering(false);
        }

        if (managedWorld.is(ManagedWorldIsQuery.ANY_ENVIRONMENTALLY_CONTROLLED, world)) {
            switch (weatherState.getCurrentWeather()) {
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {
        World world = event.getWorld();
        if (event.toThunderState() && world.getWeatherDuration() < world.getThunderDuration()) {
            world.setWeatherDuration(world.getThunderDuration());
        }

        if (managedWorld.is(ManagedWorldIsQuery.ANY_ENVIRONMENTALLY_CONTROLLED, world)) {
            switch (weatherState.getCurrentWeather()) {
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
            WeatherType currentWeather = weatherState.getCurrentWeather();
            ChatUtil.sendNotice(sender, "Currently: " + ChatColor.BLUE + currentWeather.toString());

            boolean verbose = args.hasFlag('v') && sender.hasPermission("aurora.weather.recast");
            sendWeatherPrintout(sender, weatherState.getCopiedQueue(), verbose);
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

            // Copy and clear the existing queue
            List<WeatherEvent> weatherQueue = weatherState.getCopiedQueue();
            weatherState.clearQueue();

            // Add back the modified events
            weatherState.addToQueue(new WeatherEvent(System.currentTimeMillis(), weatherTypes.poll()));
            for (WeatherEvent event : weatherQueue) {
                if (!weatherTypes.isEmpty()) {
                    event.setWeatherType(weatherTypes.poll());
                }
                weatherState.addToQueue(event);
            }

            updateWeather();

            ChatUtil.sendNotice(sender, "Current weather even changed!");
        }

        @Command(aliases = {"recast"},
                usage = "", desc = "Redo the server forecast",
                flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.weather.recast")
        public void recastCmd(CommandContext args, CommandSender sender) throws CommandException {
            weatherState.clearQueue();
            updateWeather();

            ChatUtil.sendNotice(sender, "Forecast updated!");
        }
    }
}
