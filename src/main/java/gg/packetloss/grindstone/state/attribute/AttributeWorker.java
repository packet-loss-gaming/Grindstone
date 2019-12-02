package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
import org.bukkit.entity.Player;

import java.io.IOException;

public abstract class AttributeWorker<T> {
    protected final PlayerStateAttributeImpl attribute;
    protected final PlayerStateKind kind;
    protected final PlayerStatePersistenceManager persistenceManager;

    protected AttributeWorker(PlayerStateAttributeImpl attribute, PlayerStateKind kind,
                              PlayerStatePersistenceManager persistenceManager) {
        this.attribute = attribute;
        this.kind = kind;
        this.persistenceManager = persistenceManager;
    }

    protected abstract void attach(PlayerStateRecord record, Player player) throws IOException;
    protected abstract T detach(PlayerStateRecord record, Player player);
    protected abstract void remove(T oldState, Player player) throws IOException;

    public void pushState(PlayerStateRecord record, Player player) throws IOException {
        if (attribute.isValidFor(kind, record) && kind.shouldSwapOnDuplicate()) {
            swapState(record, player);
        } else {
            attach(record, player);
        }
    }

    public void popState(PlayerStateRecord record, Player player) throws IOException {
        if (!attribute.isValidFor(kind, record)) {
            return;
        }

        remove(detach(record, player), player);
    }

    private void swapState(PlayerStateRecord record, Player player) throws IOException {
        T oldState = detach(record, player);

        attach(record, player);

        if (oldState != null) {
            remove(oldState, player);
        }
    }
}
