package gg.packetloss.grindstone.city.engine.pixieitems.db;

import java.util.Optional;
import java.util.UUID;

public interface PixieNetworkDatabase {
    Optional<Integer> createNetwork(UUID namespace, String name);
    Optional<Integer> selectNetwork(UUID namespace, String name);
}
