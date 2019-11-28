package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
import gg.packetloss.grindstone.state.PlayerVitals;
import org.bukkit.entity.Player;

import java.io.IOException;

public class VitalsAttributeImpl implements PlayerStateAttributeImpl {
    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return record.getVitals().get(kind) != null;
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return new VitalsAttributeWorker(kind, persistenceManager);
    }

    private static class VitalsAttributeWorker extends AttributeWorker<PlayerVitals> {
        protected VitalsAttributeWorker(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
            super(kind, persistenceManager);
        }

        @Override
        public void pushState(PlayerStateRecord record, Player player) throws IOException {
            record.getVitals().put(kind, new PlayerVitals(
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion(),
                    player.getTotalExperience()
            ));
        }

        @Override
        protected PlayerVitals detach(PlayerStateRecord record, Player player) {
            return record.getVitals().remove(kind);
        }

        @Override
        protected void remove(PlayerVitals oldState, Player player) throws IOException {
            player.setHealth(Math.min(player.getMaxHealth(), oldState.getHealth()));
            player.setFoodLevel(oldState.getHunger());
            player.setSaturation(oldState.getSaturation());
            player.setExhaustion(oldState.getExhaustion());
            player.setTotalExperience(oldState.getExperience());
        }
    }
}
