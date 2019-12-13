package gg.packetloss.grindstone.state.player;

import gg.packetloss.grindstone.state.player.attribute.*;

public enum PlayerStateAttribute implements PlayerStateAttributeImpl {
    VITALS(new VitalsAttributeImpl()),
    EXP(new ExperienceAttributeImpl()),
    INVENTORY(new InventoryAttributeImpl()),
    GUILD(new GuildAttributeImpl());

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
