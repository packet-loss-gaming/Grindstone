package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;

public interface PlayerStateAttributeImpl {
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record);
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager);
}
