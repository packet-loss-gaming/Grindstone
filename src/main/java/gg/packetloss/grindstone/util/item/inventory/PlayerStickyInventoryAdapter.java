package gg.packetloss.grindstone.util.item.inventory;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PlayerStickyInventoryAdapter implements InventoryAdapter {
    private final Player player;

    private final PlayerInventory playerInventory;
    private final ItemStack[] itemStacks;
    private final int[] offsetMap;
    private final boolean[] testMask;
    private final boolean[] updateMask;

    public PlayerStickyInventoryAdapter(Player player, Priority priority, Predicate<ItemStack> slotTest) {
        this.player = player;

        this.playerInventory = player.getInventory();
        this.itemStacks = playerInventory.getContents();

        // Create a mask of slots that pass the predicate, these are the slots composing this adapter
        int underlyingInvLength = playerInventory.getSize();
        this.testMask = new boolean[underlyingInvLength];
        this.updateMask = new boolean[underlyingInvLength];

        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < underlyingInvLength; ++i) {
            testMask[i] = slotTest.test(itemStacks[i + priority.getOffset() % underlyingInvLength]);
            if (testMask[i]) {
                matches.add(i);
            }
        }
        this.offsetMap = matches.stream().mapToInt(i -> i).toArray();

        Validate.isTrue(itemStacks.length == InventoryConstants.PLAYER_INV_LENGTH);
    }

    private PlayerStickyInventoryAdapter(PlayerStickyInventoryAdapter adapter) {
        this.player = adapter.player;

        this.playerInventory = adapter.playerInventory;
        this.itemStacks = ItemUtil.clone(adapter.itemStacks);
        this.offsetMap = adapter.offsetMap.clone();
        this.testMask = adapter.testMask.clone();
        this.updateMask = adapter.updateMask.clone();
    }

    @Override
    public int size() {
        return offsetMap.length;
    }

    private int translateExternalIndexToInternal(int index) {
        return offsetMap[index];
    }

    @Override
    public ItemStack getAt(int index) {
        return itemStacks[translateExternalIndexToInternal(index)];
    }

    @Override
    public void setAt(int index, ItemStack stack) {
        itemStacks[translateExternalIndexToInternal(index)] = stack;
        updateMask[translateExternalIndexToInternal(index)] = true;
    }

    @Override
    public InventoryAdapter copy() {
        return new PlayerStickyInventoryAdapter(this);
    }

    @Override
    public void applyChanges() {
        for (int i = 0; i < itemStacks.length; ++i) {
            if (updateMask[i]) {
                playerInventory.setItem(i, itemStacks[i]);
            }
        }
    }

    public enum Priority {
        HOTBAR(0),
        STORAGE(InventoryConstants.PLAYER_INV_ROW_LENGTH);

        private final int offset;

        private Priority(int offset) {
            this.offset = offset;
        }

        private int getOffset() {
            return offset;
        }
    }
}