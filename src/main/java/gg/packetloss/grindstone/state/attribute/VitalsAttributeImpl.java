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

    private static class VitalsAttributeWorker extends AttributeWorker {
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
        public void popState(PlayerStateRecord record, Player player) throws IOException {
            PlayerVitals vitals = record.getVitals().remove(kind);
            if (vitals != null) {
                player.setHealth(Math.min(player.getMaxHealth(), vitals.getHealth()));
                player.setFoodLevel(vitals.getHunger());
                player.setSaturation(vitals.getSaturation());
                player.setExhaustion(vitals.getExhaustion());
                player.setTotalExperience(vitals.getExperience());
            }
        }
    }
}
