package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
import org.bukkit.entity.Player;

import java.io.IOException;

public abstract class AttributeWorker<T> {
    protected final PlayerStateKind kind;
    protected final PlayerStatePersistenceManager persistenceManager;

    protected AttributeWorker(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        this.kind = kind;
        this.persistenceManager = persistenceManager;
    }

    public abstract void pushState(PlayerStateRecord record, Player player) throws IOException;

    protected abstract T detach(PlayerStateRecord record, Player player);
    protected abstract void remove(T oldState, Player player) throws IOException;

    public void popState(PlayerStateRecord record, Player player) throws IOException {
        remove(detach(record, player), player);
    }

    public void swapState(PlayerStateRecord record, Player player) throws IOException {
        T oldState = detach(record, player);

        pushState(record, player);

        if (oldState != null) {
            remove(oldState, player);
        }
    }
}
