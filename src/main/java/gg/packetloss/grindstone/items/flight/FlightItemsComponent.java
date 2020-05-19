package gg.packetloss.grindstone.items.flight;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.custom.item.FlightItemActivatedEvent;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ComponentInformation(friendlyName = "Flight Items Component", desc = "Flight speed management")
@Depend(components = {AdminComponent.class})
public class FlightItemsComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private AdminComponent admin;

    private Map<UUID, FlightCategory> flightProviderMapping = new HashMap<>();

    public void registerFlightProvider(Player player, FlightCategory category) {
        Validate.isTrue(player.getAllowFlight());
        Validate.isTrue(player.getFlySpeed() == category.getSpeed());

        flightProviderMapping.put(player.getUniqueId(), category);

        // This should always be created after the registration, not before.
        FlightItemActivatedEvent event = new FlightItemActivatedEvent(player, category);
        CommandBook.callEvent(event);
    }

    public Optional<FlightCategory> getFlightProvider(Player player) {
        // If not allowed to fly, the registered item is no longer providing flight.
        if (!player.getAllowFlight()) {
            return Optional.empty();
        }

        // If a flying game mode, the registered item has on effect.
        if (GeneralPlayerUtil.hasFlyingGamemode(player)) {
            return Optional.empty();
        }

        // If an admin, the registered item (may) have no effect, assume it doesn't.
        if (admin.isAdmin(player)) {
            return Optional.empty();
        }

        return Optional.ofNullable(flightProviderMapping.get(player.getUniqueId()));
    }

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        flightProviderMapping.remove(event.getPlayer().getUniqueId());
    }
}
