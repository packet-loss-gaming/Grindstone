package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
import org.bukkit.entity.Player;

import java.io.IOException;

public abstract class AttributeWorker {
    protected final PlayerStateKind kind;
    protected final PlayerStatePersistenceManager persistenceManager;

    protected AttributeWorker(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        this.kind = kind;
        this.persistenceManager = persistenceManager;
    }

    public abstract void pushState(PlayerStateRecord record, Player player) throws IOException;
    public abstract void popState(PlayerStateRecord record, Player player) throws IOException;
}
