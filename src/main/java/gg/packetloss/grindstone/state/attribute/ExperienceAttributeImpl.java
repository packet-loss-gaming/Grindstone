package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
import org.bukkit.entity.Player;

public class ExperienceAttributeImpl implements PlayerStateAttributeImpl {
    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return record.getExperience().get(kind) != null;
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return new ExperienceAttributeWorker(this, kind, persistenceManager);
    }

    private static class ExperienceAttributeWorker extends AttributeWorker<Integer> {
        protected ExperienceAttributeWorker(PlayerStateAttributeImpl attribute, PlayerStateKind kind,
                                            PlayerStatePersistenceManager persistenceManager) {
            super(attribute, kind, persistenceManager);
        }

        @Override
        public void attach(PlayerStateRecord record, Player player) {
            record.getExperience().put(kind, player.getTotalExperience());
        }

        @Override
        protected Integer detach(PlayerStateRecord record, Player player) {
            return record.getExperience().remove(kind);
        }

        @Override
        protected void remove(Integer oldState, Player player) {
            player.setTotalExperience(oldState);
        }
    }
}
