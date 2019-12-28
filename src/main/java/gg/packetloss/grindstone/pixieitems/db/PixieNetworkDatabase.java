package gg.packetloss.grindstone.pixieitems.db;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixieNetworkDatabase {
    Optional<PixieNetworkDetail> createNetwork(UUID namespace, String name);
    Optional<PixieNetworkDetail> selectNetwork(UUID namespace, String name);
    Optional<PixieNetworkDetail> selectNetwork(int networkID);
    List<PixieNetworkDetail> selectNetworks(UUID namespace);
}
