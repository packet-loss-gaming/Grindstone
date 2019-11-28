package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateAttribute;
import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;

public class TypedPlayerStateAttribute {
    private PlayerStateKind kind;
    private PlayerStateAttribute attribute;

    public TypedPlayerStateAttribute(PlayerStateKind kind, PlayerStateAttribute attribute) {
        this.kind = kind;
        this.attribute = attribute;
    }

    public boolean isValidFor(PlayerStateRecord record) {
        return attribute.isValidFor(kind, record);
    }

    public AttributeWorker getWorkerFor(PlayerStatePersistenceManager persistenceManager) {
        return attribute.getWorkerFor(kind, persistenceManager);
    }
}
