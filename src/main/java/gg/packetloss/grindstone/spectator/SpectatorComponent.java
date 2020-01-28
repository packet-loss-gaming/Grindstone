package gg.packetloss.grindstone.spectator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePrePopEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.LocationUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Spectator", desc = "Spectate away.")
public class SpectatorComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private PlayerStateComponent playerState;

    private Set<PlayerStateKind> registeredSpectatorKinds = new HashSet<>();

    private Map<PlayerStateKind, ProtectedRegion> spectatedRegion = new HashMap<>();
    private Map<Player, PlayerStateKind> playerSpectatorKind = new HashMap<>();
    private BiMap<PlayerStateKind, Location> skullLocations = HashBiMap.create();
    private List<SpectatorSkull> spectatorSkulls = new ArrayList<>();

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        server.getScheduler().runTaskTimer(inst, this::popIfOutOfBounds, 11, 1);
        server.getScheduler().runTaskTimer(inst, this::glowSkulls, 0, 7);
    }

    public void registerSpectatorKind(PlayerStateKind kind) {
        Validate.isTrue(kind.isTemporary());
        registeredSpectatorKinds.add(kind);
    }

    public void registerSpectatedRegion(PlayerStateKind kind, ProtectedRegion region) {
        Validate.isTrue(registeredSpectatorKinds.contains(kind));
        spectatedRegion.put(kind, region);
    }

    public void registerSpectatorSkull(PlayerStateKind kind, Location location, Supplier<Boolean> hasPlayers) {
        Validate.isTrue(registeredSpectatorKinds.contains(kind));

        skullLocations.put(kind, location);
        spectatorSkulls.add(new SpectatorSkull(location, hasPlayers));
    }

    private void popIfOutOfBounds() {
        Iterator<Map.Entry<Player, PlayerStateKind>> it = playerSpectatorKind.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, PlayerStateKind> entry = it.next();

            Player player = entry.getKey();
            PlayerStateKind kind = entry.getValue();

            ProtectedRegion region = spectatedRegion.get(kind);
            if (region == null) {
                continue;
            }

            if (LocationUtil.isInRegion(region, player)) {
                continue;
            }

            try {
                playerState.popState(kind, player);
                it.remove();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isSpectator(PlayerStateKind kind) {
        return registeredSpectatorKinds.contains(kind);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (!isSpectator(event.getKind())) {
            return;
        }

        Player player = event.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);

        playerSpectatorKind.put(player, event.getKind());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        Block block = event.getClickedBlock();
        assert block != null;

        PlayerStateKind stateKind = skullLocations.inverse().get(block.getLocation());
        if (stateKind == null) {
            return;
        }

        // Delay by 1 tick, players are getting teleported back because of some spectator related
        // net code in MC 1.15.1.
        server.getScheduler().runTask(inst, () -> {
            try {
                playerState.pushState(stateKind, player);
            } catch (IOException | ConflictingPlayerStateException e) {
                e.printStackTrace();
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerStatePrePop(PlayerStatePrePopEvent event) {
        Location skullLocation = skullLocations.get(event.getKind());
        if (skullLocation == null) {
            return;
        }

        BlockData blockState = skullLocation.getBlock().getBlockData();
        if (!(blockState instanceof Rotatable)) {
            return;
        }

        BlockFace skullRotation = ((Rotatable) blockState).getRotation();
        Vector skullDirection = skullRotation.getDirection();

        // Create an exit location from the skull direction
        Location exitLocation = skullLocation.clone();
        exitLocation.setDirection(skullDirection);
        exitLocation.add(skullDirection.multiply(-2));

        // Teleport player back to the skull
        Player player = event.getPlayer();
        player.teleport(LocationUtil.findFreePosition(exitLocation), PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerSpectatorKind.remove(event.getPlayer());
    }

    private void glowSkulls() {
        spectatorSkulls.forEach(SpectatorSkull::glow);
    }
}