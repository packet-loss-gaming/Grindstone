package gg.packetloss.grindstone.spectator;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePopEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import org.apache.commons.lang3.Validate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.util.*;
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

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        server.getScheduler().runTaskTimer(inst, this::popIfOutOfBounds, 20, 20);
    }

    public void registerSpectatorKind(PlayerStateKind kind) {
        Validate.isTrue(kind.isTemporary());
        registeredSpectatorKinds.add(kind);
    }

    public void registerSpectatedRegion(PlayerStateKind kind, ProtectedRegion region) {
        Validate.isTrue(registeredSpectatorKinds.contains(kind));
        spectatedRegion.put(kind, region);
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

            Location playerLoc = player.getLocation();
            if (region.contains(playerLoc.getBlockX(), playerLoc.getBlockY(), playerLoc.getBlockZ())) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (!isSpectator(event.getKind())) {
            return;
        }

        Player player = event.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerStatePop(PlayerStatePopEvent event) {
        if (!isSpectator(event.getKind())) {
            return;
        }

        Player player = event.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerSpectatorKind.remove(event.getPlayer());
    }
}