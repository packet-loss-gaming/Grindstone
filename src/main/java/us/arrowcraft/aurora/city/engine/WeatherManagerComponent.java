package us.arrowcraft.aurora.city.engine;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import us.arrowcraft.aurora.util.ChatUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Weather Manager", desc = "Turn off the storm!")
@Depend(components = {SessionComponent.class}, plugins = {"ProtocolLib"})
public class WeatherManagerComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;

    private ProtocolManager protocolManager;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        setUpProtocolManager();
    }

    private void setUpProtocolManager() {

        Plugin plugin = server.getPluginManager().getPlugin("ProtocolLib");

        if (plugin == null) protocolManager = null;
        else protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * This method is used to change the persistent status of client side weather
     *
     * @param player      - The player who you want to stop the weather for
     * @param hideWeather - Whether to hide the weather
     */
    public void hideWeather(Player player, boolean hideWeather) {

        sessions.getSession(WeatherState.class, player).setHideWeather(hideWeather);
    }

    /**
     * This method is used to change the persistent status of client side weather
     *
     * @param player        - The player who you want to stop the weather for
     * @param hideWeather   - Whether to hide the weather
     * @param updateWeather - Whether to update the weather client side immediately
     */
    public void hideWeather(Player player, boolean hideWeather, boolean updateWeather) {

        sessions.getSession(WeatherState.class, player).setHideWeather(hideWeather);
        if (hideWeather && updateWeather) stopWeather(player);
    }

    /**
     * This method is used to determine if the weather is set to be hidden from the player
     *
     * @param player - The player who you want to check the weather for
     *
     * @return - true if the weather is hidden
     */
    public boolean isWeatherHidden(Player player) {

        return sessions.getSession(WeatherState.class, player).isWeatherHidden();
    }

    /**
     * This method it used to stop the weather for a specific player on the client side end
     *
     * @param player - The player who you want to stop the weather for
     *
     * @return - true if the weather was stopped
     */
    public boolean stopWeather(Player player) {

        if (protocolManager == null) return false;

        PacketContainer fakeWeather = protocolManager.createPacket(70);
        fakeWeather.getSpecificModifier(int.class).write(0, 2).write(1, 0);
        try {
            protocolManager.sendServerPacket(player, fakeWeather);
            return true;
        } catch (InvocationTargetException e) {
            log.warning("Failed to send Packet70: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This method is used to reset the weather for a specific player on the client side end
     * ###YET TO BE IMPLEMENTED###
     *
     * @param player - The player who you want to reset the weather for
     *
     * @return - true if the weather was reset
     */
    public boolean resetWeather(Player player) {

        if (protocolManager == null) return false;
        return false;
    }

    public class Commands {

        @Command(aliases = {"stopweather"},
                usage = "", desc = "Hide all storms",
                flags = "", min = 0, max = 0)
        public void showStormCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            stopWeather((Player) sender);
            ChatUtil.sendNotice(sender, "Storms are now hidden.");
            ChatUtil.sendNotice(sender, "To show storms again please disconnect and reconnect.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if (isWeatherHidden(player)) stopWeather(player);
    }

    // Weather Session
    private static class WeatherState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private boolean hideWeather = false;

        protected WeatherState() {

            super(MAX_AGE);
        }

        public boolean isWeatherHidden() {

            return hideWeather;
        }

        public void setHideWeather(boolean hideWeather) {

            this.hideWeather = hideWeather;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}
