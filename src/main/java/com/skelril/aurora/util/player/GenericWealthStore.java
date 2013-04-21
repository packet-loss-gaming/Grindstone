package com.skelril.aurora.util.player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Turtle9598
 */
public abstract class GenericWealthStore {

    private String ownerName;
    private ItemStack[] armourContents = null;
    private ItemStack[] inventoryContents = null;
    private List<ItemStack> itemStacks = new ArrayList<>();
    private int value = 0;

    public GenericWealthStore(String ownerName, ItemStack[] inventoryContents) {

        this.ownerName = ownerName;
        this.inventoryContents = new ItemStack[inventoryContents.length];
        for (int i = 0; i < inventoryContents.length; i++) {
            this.inventoryContents[i] = inventoryContents[i] == null ? null : inventoryContents[i].clone();
        }
    }

    public GenericWealthStore(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents) {

        this.ownerName = ownerName;
        this.inventoryContents = new ItemStack[inventoryContents.length];
        for (int i = 0; i < inventoryContents.length; i++) {
            this.inventoryContents[i] = inventoryContents[i] == null ? null : inventoryContents[i].clone();
        }
        this.armourContents = new ItemStack[armourContents.length];
        for (int i = 0; i < armourContents.length; i++) {
            this.armourContents[i] = armourContents[i] == null ? null : armourContents[i].clone();
        }
    }

    public GenericWealthStore(String ownerName, List<ItemStack> itemStacks) {

        this.ownerName = ownerName;
        this.itemStacks = itemStacks;
    }

    public GenericWealthStore(String ownerName, List<ItemStack> itemStacks, int value) {

        this.ownerName = ownerName;
        this.itemStacks = itemStacks;
        this.value = value;
    }

    public GenericWealthStore(String ownerName, int value) {

        this.ownerName = ownerName;
        this.value = value;
    }

    public String getOwnerName() {

        return ownerName;
    }

    public void setOwnerName(String ownerName) {

        this.ownerName = ownerName;
    }

    public ItemStack[] getArmourContents() {

        return armourContents;
    }

    public void setArmourContents(ItemStack[] armourContents) {

        this.armourContents = new ItemStack[armourContents.length];
        for (int i = 0; i < armourContents.length; i++) {
            this.armourContents[i] = armourContents[i] == null ? null : armourContents[i].clone();
        }
    }

    public ItemStack[] getInventoryContents() {

        return inventoryContents;
    }

    public void setInventoryContents(ItemStack[] inventoryContents) {

        this.inventoryContents = new ItemStack[inventoryContents.length];
        for (int i = 0; i < inventoryContents.length; i++) {
            this.inventoryContents[i] = inventoryContents[i] == null ? null : inventoryContents[i].clone();
        }
    }

    public List<ItemStack> getItemStacks() {

        return itemStacks;
    }

    public void setItemStacks(List<ItemStack> itemStacks) {

        this.itemStacks = itemStacks;
    }

    public int getValue() {

        return value;
    }

    public void setValue(int value) {

        this.value = value;
    }

}
