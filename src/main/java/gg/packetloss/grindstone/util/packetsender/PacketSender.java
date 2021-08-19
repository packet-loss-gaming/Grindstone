package gg.packetloss.grindstone.util.packetsender;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.entity.Entity;

public class PacketSender {
    private final PacketContainer packetContainer;
    private final WrappedDataWatcher watcher;
    private final WrappedDataWatcher.Serializer serializer;

    public PacketSender(PacketType packetType) {
        packetContainer = ProtocolLibrary.getProtocolManager().createPacket(packetType);
        watcher = new WrappedDataWatcher();
        serializer = WrappedDataWatcher.Registry.get(Byte.class);
    }

    public void setEntity(Entity entity) {
        packetContainer.getIntegers().write(0, entity.getEntityId());
        watcher.setEntity(entity);
    }

    public void setWatcherObject(int index, byte packetVal) {
        watcher.setObject(index, serializer, packetVal);
        packetContainer.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
    }

    public void broadcastServerPacket(Entity entity) {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(packetContainer, entity, true);
    }

}
