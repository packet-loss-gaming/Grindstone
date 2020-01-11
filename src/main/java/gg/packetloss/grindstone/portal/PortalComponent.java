package gg.packetloss.grindstone.portal;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.firstlogin.FirstLoginComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.managedworld.ManagedWorldIsQuery;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.warps.WarpsComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ComponentInformation(friendlyName = "Portal", desc = "Portal support.")
@Depend(components = {FirstLoginComponent.class, ManagedWorldComponent.class, PlayerHistoryComponent.class, WarpsComponent.class})
public class PortalComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private FirstLoginComponent firstLogin;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PlayerHistoryComponent playerHistory;
    @InjectComponent
    private WarpsComponent warps;

    private Map<PortalDestinationType, WorldResolver> worldTypeLookup = new HashMap<>();
    private Map<Material, PortalDestinationType> portalToType = new HashMap<>();

    private void initWorldLookup() {
        worldTypeLookup.put(PortalDestinationType.CITY, new SimpleWorldResolver(managedWorld, ManagedWorldGetQuery.CITY, warps));
        worldTypeLookup.put(PortalDestinationType.BUILD, new BuildWorldResolver(managedWorld, ManagedWorldGetQuery.LATEST_BUILD, warps, firstLogin));
        worldTypeLookup.put(PortalDestinationType.SKY, new SkyWorldResolver(managedWorld, ManagedWorldGetQuery.SKY, warps, playerHistory));
        worldTypeLookup.put(PortalDestinationType.WILDERNESS, new SimpleWorldResolver(managedWorld, ManagedWorldGetQuery.WILDERNESS, warps));
    }

    private void initTypeMapping() {
        portalToType.put(Material.COBBLESTONE, PortalDestinationType.BUILD);
        portalToType.put(Material.IRON_BLOCK, PortalDestinationType.WILDERNESS);
        portalToType.put(Material.GOLD_BLOCK, PortalDestinationType.CITY);
        portalToType.put(Material.EMERALD_BLOCK, PortalDestinationType.SKY);
    }

    @Override
    public void enable() {
        initWorldLookup();
        initTypeMapping();

        CommandBook.registerEvents(this);
    }

    private Location getConsistentFrom(Location loc) {
        Location xTestLoc = loc.clone();

        while (true) {
            if (xTestLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                xTestLoc.add(-1, 0, 0);
                continue;
            }

            break;
        }

        Location zTestLoc = loc.clone();
        while (true) {
            if (zTestLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                zTestLoc.add(0, 0, -1);
                continue;
            }

            break;
        }

        return new Location(loc.getWorld(), xTestLoc.getBlockX() + 1, loc.getY(), zTestLoc.getBlockZ() + 1);
    }

    private Optional<WorldResolver> getOverrideType(Location from) {
        Block fromBlock = from.getBlock();
        while (fromBlock.getType() == Material.NETHER_PORTAL) {
            fromBlock = fromBlock.getRelative(BlockFace.DOWN);
        }

        return Optional.ofNullable(portalToType.get(fromBlock.getType())).map(type -> worldTypeLookup.get(type));
    }

    private void redirectPortalNoAgent(PlayerPortalEvent event, Location destination) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
    }

    private void redirectPortalWithAgent(PlayerPortalEvent event, Location destination) {
        event.setTo(destination);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return;
        }

        Player player = event.getPlayer();
        Location from = getConsistentFrom(event.getFrom());

        World fromWorld = from.getWorld();

        Optional<WorldResolver> remappingFunction = getOverrideType(from);
        if (remappingFunction.isPresent()) {
            WorldResolver resolver = remappingFunction.get();
            if (!resolver.accepts(player)) {
                ChatUtil.sendError(player, "The world gate refuses to let you through.");
                return;
            }

            Location targetLocation = resolver.getDestinationFor(player);
            redirectPortalNoAgent(event, targetLocation);

            return;
        }

        // Wilderness Code
        if (managedWorld.is(ManagedWorldIsQuery.WILDERNESS, fromWorld)) {
            redirectPortalWithAgent(event, new Location(
                    managedWorld.get(ManagedWorldGetQuery.WILDERNESS_NETHER),
                    from.getX() / 8,
                    from.getBlockY(),
                    from.getZ() / 8
            ));
            return;
        }

        if (managedWorld.is(ManagedWorldIsQuery.WILDERNESS_NETHER, fromWorld)) {
            redirectPortalWithAgent(event, new Location(
                    managedWorld.get(ManagedWorldGetQuery.WILDERNESS),
                    from.getX() / 8,
                    from.getBlockY(),
                    from.getZ() / 8
            ));
            return;
        }

        // City fallback Code
        if (managedWorld.is(ManagedWorldIsQuery.ANY_BUILD, fromWorld)) {
            redirectPortalNoAgent(event, worldTypeLookup.get(PortalDestinationType.CITY).getDestinationFor(player));
            return;
        }

        if (managedWorld.is(ManagedWorldIsQuery.CITY, fromWorld)) {
            redirectPortalNoAgent(event, worldTypeLookup.get(PortalDestinationType.BUILD).getDestinationFor(player));
            return;
        }
    }

    // FIXME: Fix this
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        event.setCancelled(true);
    }


    private void tryCreatePortal(Player player, Block block) {
        Material blockType = block.getRelative(BlockFace.DOWN).getType();

        boolean isPortalIgnitingBlock = portalToType.containsKey(blockType);
        if (!isPortalIgnitingBlock) {
            return;
        }

        new PortalGenerator((opBlock) -> WorldGuardBridge.canBuildAt(player, opBlock), block, blockType).walk();
    }

    @EventHandler
    public void onBlockIgnite(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.FIRE) {
            return;
        }

        tryCreatePortal(event.getPlayer(), event.getBlockPlaced());
    }
}
