package gg.packetloss.grindstone.state.player.attribute;

import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.state.player.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.player.PlayerStateRecord;

public interface PlayerStateAttributeImpl {
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record);
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager);
}
