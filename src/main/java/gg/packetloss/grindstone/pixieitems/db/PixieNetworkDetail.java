package gg.packetloss.grindstone.pixieitems.db;

import java.util.UUID;

public class PixieNetworkDetail implements Comparable<PixieNetworkDetail> {
    private final int networkID;
    private final UUID namespace;
    private final String name;

    public PixieNetworkDetail(int networkID, UUID namespace, String name) {
        this.networkID = networkID;
        this.namespace = namespace;
        this.name = name;
    }

    public int getID() {
        return networkID;
    }

    public UUID getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(PixieNetworkDetail network) {
        if (network == null) return -1;
        return this.getName().compareTo(network.getName());
    }
}
