package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.HomeTeleportEvent;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.util.Optional;
import java.util.logging.Logger;

@ComponentInformation(
        friendlyName = "Rift Warps",
        desc = "Provides warps functionality"
)
public class WarpsComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private WarpManager warpManager;

    protected WarpManager getWarpManager() {
        return warpManager;
    }

    @Override
    public void enable() {
        CommandBook.registerEvents(this);

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        WarpPointConverter.register(registrar, this);
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            //  WarpPointConverter.register(commandManager, this);
            registration.register(commandManager, WarpCommandsRegistration.builder(), new WarpCommands(this));

            registrar.registerAsSubCommand("warps", "Warp management", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, WarpManagementCommandsRegistration.builder(), new WarpManagementCommands(this));
            });
        });

        File warpsDirectory = new File(inst.getDataFolder().getPath() + "/warps");
        if (!warpsDirectory.exists()) warpsDirectory.mkdir();

        WarpDatabase warpDatabase = new CSVWarpDatabase("warps", warpsDirectory);
        warpDatabase.load();

        warpManager = new WarpManager(warpDatabase);
    }

    public Optional<Location> getRawBedLocation(Player player) {
        return warpManager.getHomeFor(player).map(WarpPoint::getLocation);
    }

    public Optional<Location> getBedLocation(Player player) {
        return warpManager.getHomeFor(player).map(WarpPoint::getSafeLocation);
    }

    public Optional<Location> getLastPortalLocation(Player player, World world) {
        return warpManager.getLastPortalLocationFor(player, world).map(WarpPoint::getSafeLocation);
    }

    public Location getRespawnLocation(Player player) {
        Location spawnLoc = player.getWorld().getSpawnLocation();
        return getBedLocation(player).orElse(spawnLoc);
    }

    // FIXME: Priority set as workaround for Multiverse-Core#1977
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        HomeTeleportEvent HTE = new HomeTeleportEvent(event.getPlayer(), getRespawnLocation(event.getPlayer()));
        server.getPluginManager().callEvent(HTE);
        if (!HTE.isCancelled()) event.setRespawnLocation(HTE.getDestination());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortal(PlayerTeleportEvent event) {
        // Use this over the nether portal check, because we need to pay attention to redirects.
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();

        World.Environment fromEnvironment = fromWorld.getEnvironment();
        World.Environment toEnvironment = toWorld.getEnvironment();

        // Do not record world changes that involve a nether. This may be reconsidered in the future.
        if (fromEnvironment == World.Environment.NETHER || toEnvironment == World.Environment.NETHER) {
            return;
        }

        Player player = event.getPlayer();

        // Invert the view location to make walking through portals feel more natural.
        Location invertedViewLocation = from.clone();
        invertedViewLocation.setDirection(from.getDirection().multiply(-1).setY(0));

        warpManager.setLastPortalLocation(player, invertedViewLocation);
    }

    private boolean canSetPlayerBed(Location loc) {
        return !loc.getWorld().getName().toLowerCase().contains("legit");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Location bedLoc = event.getBed().getLocation();

        if (player == null || bedLoc == null) {
            return;
        }
        if (!canSetPlayerBed(bedLoc)) {
            return;
        }

        warpManager.setPlayerHomeAndNotify(player, bedLoc);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRightClickBed(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!EnvironmentUtil.isBed(block)) {
            return;
        }
        if (!canSetPlayerBed(block.getLocation()))  {
            return;
        }

        warpManager.setPlayerHomeAndNotify(player, block.getLocation());
    }
}
