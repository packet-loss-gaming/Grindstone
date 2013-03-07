package us.arrowcraft.aurora.city.engine;

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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import us.arrowcraft.aurora.admin.AdminComponent;
import us.arrowcraft.aurora.admin.AdminState;
import us.arrowcraft.aurora.events.*;
import us.arrowcraft.aurora.homes.CSVHomeDatabase;
import us.arrowcraft.aurora.homes.EnderPearlHomesComponent;
import us.arrowcraft.aurora.homes.HomeDatabase;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Legit Core", desc = "Operate the legit world.")
@Depend(components = {AdminComponent.class, EnderPearlHomesComponent.class})
public class LegitCoreComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private EnderPearlHomesComponent mainHomeDatabase;

    private LocalConfiguration config;
    private HomeDatabase homeDatabase;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);

        File homeDirectory = new File(inst.getDataFolder().getPath() + "/home");
        if (!homeDirectory.exists()) homeDirectory.mkdir();

        homeDatabase = new CSVHomeDatabase("legithomes", homeDirectory);
        homeDatabase.load();
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    public Location getBedLocation(Player player) {

        Location bedLocation = null;
        if (homeDatabase.houseExist(player.getName())) {
            bedLocation = LocationUtil.findFreePosition(homeDatabase.getHouse(player.getName()).getLocation());
        }
        return bedLocation != null ? bedLocation : null;
    }

    public Location getRespawnLocation(Player player) {

        Location respawnLoc = Bukkit.getWorld(config.legitWorld).getSpawnLocation();
        return getBedLocation(player) != null ? getBedLocation(player) : respawnLoc;
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("city-world")
        public String cityWorld = "City";
        @Setting("wilderness-world")
        public String legitWorld = "Legit";
    }

    public class Commands {

        @Command(aliases = {"legit", "seemslegit"}, desc = "Enter Legit World",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"multiverse.access.Legit", "multiverse.access.Legit_nether"})
        public void toggleLegitCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            if (player.getWorld().getName().contains(config.legitWorld)) {
                try {
                    Location l = mainHomeDatabase.getBedLocation(player);
                    if (l != null && !l.getWorld().getName().contains(config.legitWorld)) {
                        player.teleport(l);
                    } else {
                        player.teleport(Bukkit.getWorld(config.cityWorld).getSpawnLocation());
                    }
                } catch (Exception e) {
                    log.warning("Please verify the world: " + config.cityWorld + "exists.");
                }
            } else {
                try {
                    player.teleport(getRespawnLocation(player));
                } catch (Exception e) {
                    log.warning("Please verify the world: " + config.legitWorld + "exists.");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdminModeChange(PlayerAdminModeChangeEvent event) {

        World world = event.getPlayer().getWorld();

        if (!event.getNewAdminState().equals(AdminState.MEMBER) && world.getName().contains(config.legitWorld)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        World world = event.getPlayer().getWorld();

        if (world.getName().contains(config.legitWorld) && event.getCause().getEffect().getType().isHoly()) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEggDrop(EggDropEvent event) {

        World world = event.getLocation().getWorld();

        if (world.getName().contains(config.legitWorld)) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        if (event.getTo().getWorld() != event.getFrom().getWorld()) {

            check(event.getPlayer(), event.getFrom().getWorld().getName(), event.getTo().getWorld().getName());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

        check(player, event.getFrom().getName(), player.getWorld().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHomeTeleport(HomeTeleportEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.legitWorld)) {

            event.setDestination(getRespawnLocation(player));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseBedSpawn(ApocalypseBedSpawnEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.legitWorld) && getBedLocation(player) != null) {

            event.setLocation(getBedLocation(player));
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {

        Player player = event.getPlayer();
        Location bedLoc = event.getBed().getLocation();

        if (player == null || bedLoc == null) return;
        if (!player.getWorld().getName().toLowerCase().contains("legit")) return;

        boolean overWritten = false;

        if (homeDatabase.houseExist(player.getName())) {
            homeDatabase.deleteHouse(player.getName());
            overWritten = homeDatabase.save();
        }

        homeDatabase.saveHouse(player, bedLoc.getWorld().getName(), bedLoc.getBlockX(), bedLoc.getBlockY(),
                bedLoc.getBlockZ());
        if (homeDatabase.save()) {
            if (!overWritten) ChatUtil.sendNotice(player, "Your bed location has been set.");
            else ChatUtil.sendNotice(player, "Your bed location has been changed.");
        }
    }

    private Set<Player> playerList = new HashSet<>();

    public void check(final Player player, String from, String to) {

        if (to.contains(config.legitWorld) && !from.contains(config.legitWorld)) {

            adminComponent.standardizePlayer(player);
            if (!playerList.contains(player)) ChatUtil.sendNotice(player, "You have entered legit world.");
            playerList.add(player);
        } else if (from.contains(config.legitWorld) && !to.contains(config.legitWorld)) {

            if (!playerList.contains(player)) ChatUtil.sendNotice(player, "You have left legit world.");
            playerList.add(player);
        }

        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                if (playerList.contains(player)) {

                    playerList.remove(player);
                }
            }
        }, 40);
    }

    // Catch possible escapes
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.legitWorld)) adminComponent.standardizePlayer(player);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.legitWorld) && event.isFlying()) event.setCancelled(true);
    }
}