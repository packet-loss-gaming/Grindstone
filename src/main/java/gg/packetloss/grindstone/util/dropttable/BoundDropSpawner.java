package gg.packetloss.grindstone.util.dropttable;

import gg.packetloss.grindstone.ProtectedDroppedItemsComponent;
import org.bukkit.Location;
import org.bukkit.entity.Item;

import java.util.function.Supplier;

public class BoundDropSpawner implements DropProvider {
    private final ProtectedDroppedItemsComponent dropProtector;
    private final Supplier<Location> dropDestination;

    public BoundDropSpawner(ProtectedDroppedItemsComponent dropProtector, Supplier<Location> dropDestination1) {
        this.dropProtector = dropProtector;
        this.dropDestination = dropDestination1;
    }

    @Override
    public void provide(DropTable dropTable, KillInfo killInfo) {
        dropTable.getDrops(killInfo, (drop) -> {
            Location destination  = dropDestination.get();
            Item item = destination.getWorld().dropItem(destination, drop.getDrop());

            drop.getPlayer().ifPresent(player -> dropProtector.protectDrop(item, player));
        });
    }
}
