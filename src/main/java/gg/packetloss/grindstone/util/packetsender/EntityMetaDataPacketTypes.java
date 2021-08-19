package gg.packetloss.grindstone.util.packetsender;

public enum EntityMetaDataPacketTypes {
    DEFAULT_HAND_STATE(0x00),
    RIPTIDE(0x04);

    private final int value;

    public byte getValue() {
        return (byte) value;
    }

    EntityMetaDataPacketTypes(int i) {
        value = i;
    }
}
