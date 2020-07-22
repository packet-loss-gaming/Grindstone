/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.attribute;

import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.state.player.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.player.PlayerStateRecord;
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
