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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Better Weather", desc = "Improves weather mechanics.")
public class BetterWeatherComponent extends BukkitComponent implements Runnable, Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private Deque<WeatherEvent> weatherQueue = new LinkedList<>();

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
        if (weatherQueue.size() < 2) {
            return;
        }

        if (!weatherQueue.peek().shouldActivate()) {
            return;
        }

        WeatherEvent event = weatherQueue.poll();

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
        weatherTypes.add(WeatherType.THUNDER_STORM, config.thunderStormStormTypeWeight);

        return weatherTypes.pick();
    }

    private void populateWeatherQueue() {
        long nextWeatherEvent = weatherQueue.isEmpty() ? System.currentTimeMillis()
                                                       : weatherQueue.getLast().getActivationTime();

        while (weatherQueue.size() < config.numToCreate) {
            long offset = TimeUnit.MINUTES.toMillis(ChanceUtil.getRangedRandom(config.shortestEvent, config.longestEvent));
            nextWeatherEvent += offset;

            WeatherType newWeather = pickWeather();

            // Queue a bit of rain as a warning about the impending thunderstorm
            if (newWeather == WeatherType.THUNDER_STORM) {
                weatherQueue.add(new WeatherEvent(nextWeatherEvent, WeatherType.RAIN));

                nextWeatherEvent += ChanceUtil.getRandomNTimes(
                        config.shortestThunderWarning, config.longestThunderWarning
                );
            }

            weatherQueue.add(new WeatherEvent(nextWeatherEvent, newWeather));
        }
    }

    @Override
    public void run() {
        changeWeather();
        populateWeatherQueue();
    }

    @EventHandler(ignoreCancelled = true)
    public void onStormChange(WeatherChangeEvent event) {
        if (!event.toWeatherState() && event.getWorld().isThundering()) {
            event.getWorld().setThundering(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {
        World world = event.getWorld();
        if (event.toThunderState() && world.getWeatherDuration() < world.getThunderDuration()) {
            world.setStorm(true);
            world.setWeatherDuration(world.getThunderDuration());
        }
    }

    public class Commands {
        @Command(aliases = {"forecast"},
                usage = "", desc = "Get the forecast for the server's weather",
                flags = "", min = 0, max = 0)
        public void forecastCmd(CommandContext args, CommandSender sender) throws CommandException {
            WeatherType lastWeatherType = null;
            int printed = 0;

            for (WeatherEvent event : weatherQueue) {
                if (printed == config.numToPrint) {
                    break;
                }

                if (printed == 0 && sender instanceof Player) {
                    World world = ((Player) sender).getWorld();
                    WeatherType weatherType = event.getWeatherType();

                    boolean stormyStateMatches = weatherType.isStorm() == world.hasStorm();
                    boolean thunderStateMatches = weatherType.hasThunder() == world.isThundering();
                    if (stormyStateMatches && thunderStateMatches) {
                        continue;
                    }
                }

                WeatherType weatherType = event.getWeatherType();
                if (lastWeatherType == weatherType) {
                    continue;
                }


                long activationTime = event.getActivationTime();
                String weatherTypeName = weatherType.name();

                ChatUtil.sendNotice(
                        sender,
                        " - " + TimeUtil.getPrettyTime(activationTime) + " " + ChatColor.BLUE + weatherTypeName
                );

                lastWeatherType = weatherType;
                ++printed;
            }
        }

        @Command(aliases = {"recast"},
                usage = "", desc = "Redo the server forecast",
                flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.weather.recast")
        public void showStormCmd(CommandContext args, CommandSender sender) throws CommandException {
            weatherQueue.clear();
            populateWeatherQueue();

            ChatUtil.sendNotice(sender, "Forecast updated!");
        }
    }
}
