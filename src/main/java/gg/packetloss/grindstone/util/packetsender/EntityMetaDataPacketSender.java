package gg.packetloss.grindstone.util.packetsender;

import com.comphenix.protocol.PacketType;

public class EntityMetaDataPacketSender extends PacketSender {
    public EntityMetaDataPacketSender() {
        super(PacketType.Play.Server.ENTITY_METADATA);
    }
}
