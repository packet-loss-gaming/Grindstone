package gg.packetloss.grindstone.city.engine.pixieitems.db;

import java.util.Optional;
import java.util.UUID;

public interface PixieNetworkDatabase {
    Optional<PixieNetworkDetail> createNetwork(UUID namespace, String name);
    Optional<PixieNetworkDetail> selectNetwork(UUID namespace, String name);
    Optional<PixieNetworkDetail> selectNetwork(int networkID);
}
