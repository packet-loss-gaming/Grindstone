/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.attribute;

import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.state.player.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.player.PlayerStateRecord;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GameModeAttributeImpl implements PlayerStateAttributeImpl {
    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return record.getGameModes().get(kind) != null;
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return new GameModeAttributeWorker(this, kind, persistenceManager);
    }

    private static class GameModeAttributeWorker extends AttributeWorker<GameMode> {
        protected GameModeAttributeWorker(PlayerStateAttributeImpl attribute, PlayerStateKind kind,
                                          PlayerStatePersistenceManager persistenceManager) {
            super(attribute, kind, persistenceManager);
        }

        @Override
        public void attach(PlayerStateRecord record, Player player) {
            record.getGameModes().put(kind, player.getGameMode());
        }

        @Override
        protected GameMode detach(PlayerStateRecord record, Player player) {
            return record.getGameModes().remove(kind);
        }

        @Override
        protected void remove(GameMode gameMode, Player player) {
            player.setGameMode(gameMode);
        }
    }
}
