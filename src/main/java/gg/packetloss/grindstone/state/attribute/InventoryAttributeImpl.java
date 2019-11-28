package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class InventoryAttributeImpl implements PlayerStateAttributeImpl {
    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return record.getInventories().get(kind) != null;
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return new InventoryAttributeWorker(kind, persistenceManager);
    }

    private static class InventoryAttributeWorker extends AttributeWorker {
        protected InventoryAttributeWorker(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
            super(kind, persistenceManager);
        }

        @Override
        public void pushState(PlayerStateRecord record, Player player) throws IOException {
            UUID inventoryID = UUID.randomUUID();
            record.getInventories().put(kind, inventoryID);

            persistenceManager.addInventory(inventoryID, player);
        }

        @Override
        public void popState(PlayerStateRecord record, Player player) throws IOException {
            UUID inventory = record.getInventories().remove(kind);
            if (inventory != null) {
                List<ItemStack> contents = persistenceManager.removeInventory(inventory);

                player.getInventory().setContents(contents.toArray(new ItemStack[0]));
                player.updateInventory();
            }
        }
    }
}
