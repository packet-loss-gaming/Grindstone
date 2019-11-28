package gg.packetloss.grindstone.state;

import gg.packetloss.grindstone.state.attribute.AttributeWorker;
import gg.packetloss.grindstone.state.attribute.InventoryAttributeImpl;
import gg.packetloss.grindstone.state.attribute.PlayerStateAttributeImpl;
import gg.packetloss.grindstone.state.attribute.VitalsAttributeImpl;

public enum PlayerStateAttribute implements PlayerStateAttributeImpl {
    VITALS(new VitalsAttributeImpl()),
    INVENTORY(new InventoryAttributeImpl());

    private final PlayerStateAttributeImpl impl;

    private PlayerStateAttribute(PlayerStateAttributeImpl impl) {
        this.impl = impl;
    }

    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return impl.isValidFor(kind, record);
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return impl.getWorkerFor(kind, persistenceManager);
    }
}
