package gg.packetloss.grindstone.state;

import gg.packetloss.grindstone.state.attribute.*;

public enum PlayerStateAttribute implements PlayerStateAttributeImpl {
    VITALS(new VitalsAttributeImpl()),
    EXP(new ExperienceAttributeImpl()),
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
