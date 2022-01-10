/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.firstlogin.FirstLoginComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.timetravel.TimeTravelComponent;
import gg.packetloss.grindstone.world.type.sky.SkyWorldCoreComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@ComponentInformation(friendlyName = "Portal", desc = "Portal support.")
@Depend(components = {FirstLoginComponent.class, ManagedWorldComponent.class,
        SkyWorldCoreComponent.class, TimeTravelComponent.class, WarpsComponent.class})
public class PortalComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private FirstLoginComponent firstLogin;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private SkyWorldCoreComponent skyWorldCore;
    @InjectComponent
    private TimeTravelComponent timeTravel;
    @InjectComponent
    private WarpsComponent warps;

    private Map<PortalDestinationType, WorldResolver> worldTypeLookup = new HashMap<>();
    private Map<Material, PortalDestinationType> portalToType = new HashMap<>();

    private void initWorldLookup() {
        worldTypeLookup.put(
            PortalDestinationType.RANGE,
            new RangeWorldResolver(managedWorld, ManagedWorldGetQuery.RANGE_OVERWORLD, warps, firstLogin)
        );
        worldTypeLookup.put(
            PortalDestinationType.TIME_MACHINE,
            new RangeWorldResolver(
                managedWorld,
                ManagedWorldGetQuery.RANGE_OVERWORLD,
                warps,
                firstLogin,
                (player) -> timeTravel.getSelectedArchivePeriod(player)
            )
        );
        worldTypeLookup.put(
            PortalDestinationType.CITY,
            new SimpleWorldResolver(managedWorld, ManagedWorldGetQuery.CITY, warps)
        );
        worldTypeLookup.put(
            PortalDestinationType.SKY,
            new SkyWorldResolver(managedWorld, ManagedWorldGetQuery.SKY, warps, skyWorldCore)
        );
    }

    private void initTypeMapping() {
        portalToType.put(Material.COBBLESTONE, PortalDestinationType.RANGE);
        portalToType.put(Material.IRON_BLOCK, PortalDestinationType.TIME_MACHINE);
        portalToType.put(Material.STONE_BRICKS, PortalDestinationType.CITY);
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

    private static class AccompanyingEntity {
        private final LivingEntity entity;
        private final boolean leashed;

        public AccompanyingEntity(LivingEntity entity, boolean isLeashed) {
            this.entity = entity;
            this.leashed = isLeashed;
        }

        public void followPlayerTeleport(Player player) {
            boolean wasTeleported = entity.teleport(player, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
            if (leashed) {
                if (wasTeleported) {
                    entity.setLeashHolder(player);
                } else {
                    entity.getWorld().dropItem(entity.getLocation(), new ItemStack(Material.LEAD));
                }
            }
        }

        public void playerTeleportCanceled(Player player) {
            if (leashed) {
                entity.setLeashHolder(player);
            }
        }
    }

    private List<AccompanyingEntity> getAccompanyingEntities(Player player) {
        List<AccompanyingEntity> accompanying = new ArrayList<>();

        for (LivingEntity entity: player.getLocation().getNearbyLivingEntities(16)) {
            if (entity.equals(player)) {
                continue;
            }

            if (entity.isLeashed() && entity.getLeashHolder().equals(player)) {
                entity.setLeashHolder(null);
                accompanying.add(new AccompanyingEntity(entity, true));
                continue;
            }

            if (EntityUtil.willFollowOwner(entity)) {
                if (entity instanceof Sittable && ((Sittable) entity).isSitting()) {
                    continue;
                }

                AnimalTamer tamer = ((Tameable) entity).getOwner();
                if (tamer == null) {
                    continue;
                }

                if (tamer.getUniqueId().equals(player.getUniqueId())) {
                    accompanying.add(new AccompanyingEntity(entity, false));
                }
            }
        }

        return accompanying;
    }

    private void redirectPortalNoAgent(PlayerPortalEvent event, Location destination) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        List<AccompanyingEntity> accompanying = getAccompanyingEntities(player);

        player.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL).thenAccept((teleported) -> {
            if (teleported) {
                for (AccompanyingEntity entity : accompanying) {
                    entity.followPlayerTeleport(player);
                }
            } else {
                for (AccompanyingEntity entity : accompanying) {
                    entity.playerTeleportCanceled(player);
                }
            }
        });

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
                event.setCancelled(true);
                ChatUtil.sendError(player, "The world gate refuses to let you through.");
                return;
            }

            Location targetLocation = resolver.getDestinationFor(player);
            if (targetLocation.getWorld() == fromWorld) {
                event.setCancelled(true);
                ChatUtil.sendError(player, "That portal points to this world!");
                ChatUtil.sendError(player, "You can't go through there, you might blow up the universe!");
                return;
            }

            redirectPortalNoAgent(event, targetLocation);

            return;
        }

        // Range Code
        if (managedWorld.is(ManagedWorldIsQuery.RANGE_OVERWORLD, fromWorld)) {
            redirectPortalWithAgent(event, new Location(
                    managedWorld.get(
                        ManagedWorldGetQuery.RANGE_NETHER,
                        managedWorld.getTimeContextFor(player.getWorld())
                    ),
                    from.getX() / 8,
                    from.getBlockY(),
                    from.getZ() / 8
            ));
            return;
        }

        if (managedWorld.is(ManagedWorldIsQuery.RANGE_NETHER, fromWorld)) {
            redirectPortalWithAgent(event, new Location(
                    managedWorld.get(
                        ManagedWorldGetQuery.RANGE_OVERWORLD,
                        managedWorld.getTimeContextFor(player.getWorld())
                    ),
                    from.getX() * 8,
                    from.getBlockY(),
                    from.getZ() * 8
            ));
            return;
        }

        // City fallback Code
        if (managedWorld.is(ManagedWorldIsQuery.CITY, fromWorld)) {
            redirectPortalNoAgent(event, worldTypeLookup.get(PortalDestinationType.RANGE).getDestinationFor(player));
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
