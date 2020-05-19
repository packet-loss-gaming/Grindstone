package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class BoundDropSpawner implements DropProvider {
    private final Supplier<Location> dropDestination;

    public BoundDropSpawner(Supplier<Location> dropDestination1) {
        this.dropDestination = dropDestination1;
    }

    private void protectDrop(Item item, Player player) {
        item.setOwner(player.getUniqueId());

        // Prevent environmental shenanigans
        item.setInvulnerable(true);
        item.setCanMobPickup(false);
    }

    @Override
    public <T extends KillInfo> void provide(DropTable<T> dropTable, T killInfo) {
        dropTable.getDrops(killInfo, (drop) -> {
            Location destination  = dropDestination.get();
            Item item = destination.getWorld().dropItem(destination, drop.getDrop());

            drop.getPlayer().ifPresent(player -> protectDrop(item, player));
        });
    }
}
