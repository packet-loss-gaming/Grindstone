package gg.packetloss.grindstone.util.item.inventory;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerStoragePriorityInventoryAdapter implements InventoryAdapter {
    private Player player;
    private PlayerInventory playerInventory;
    private ItemStack[] itemStacks;

    public PlayerStoragePriorityInventoryAdapter(Player player) {
        this.player = player;

        this.playerInventory = player.getInventory();
        this.itemStacks = playerInventory.getContents();

        Validate.isTrue(itemStacks.length == InventoryConstants.PLAYER_INV_LENGTH);
    }

    private PlayerStoragePriorityInventoryAdapter(PlayerStoragePriorityInventoryAdapter adapter) {
        this.player = adapter.player;

        this.playerInventory = adapter.playerInventory;
        this.itemStacks = ItemUtil.clone(adapter.itemStacks);
    }

    @Override
    public int size() {
        return InventoryConstants.PLAYER_INV_LENGTH - InventoryConstants.PLAYER_INV_ARMOUR_LENGTH;
    }

    private int inflateExternalIndex(int index) {
        if (index >= InventoryConstants.PLAYER_INV_STORAGE_LENGTH) {
            return index + InventoryConstants.PLAYER_INV_ARMOUR_LENGTH;
        }
        return index;
    }

    private int prioritizeInternalIndex(int index) {
        if (index > InventoryConstants.PLAYER_INV_STORAGE_LENGTH) {
            return index;
        }
        return (index + InventoryConstants.PLAYER_INV_ROW_LENGTH) % InventoryConstants.PLAYER_INV_STORAGE_LENGTH;
    }

    private int translateExternalIndexToInternal(int index) {
        return prioritizeInternalIndex(inflateExternalIndex(index));
    }

    @Override
    public ItemStack getAt(int index) {
        return itemStacks[translateExternalIndexToInternal(index)];
    }

    @Override
    public void setAt(int index, ItemStack stack) {
        itemStacks[translateExternalIndexToInternal(index)] = stack;
    }

    @Override
    public InventoryAdapter copy() {
        return new PlayerStoragePriorityInventoryAdapter(this);
    }

    @Override
    public void applyChanges() {
        playerInventory.setContents(itemStacks);
        player.updateInventory();
    }
}
