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
