/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

class RelocatedInventory implements Inventory {
    private final Inventory inventory;
    private final Location location;

    RelocatedInventory(Inventory inventory, Location location) {
        this.inventory = inventory;
        this.location = location;
    }

    @Override
    public int getSize() {
        return inventory.getSize();
    }

    @Override
    public int getMaxStackSize() {
        return inventory.getMaxStackSize();
    }

    @Override
    public void setMaxStackSize(int size) {
        inventory.setMaxStackSize(size);
    }

    @Override
    public @Nullable ItemStack getItem(int index) {
        return inventory.getItem(index);
    }

    @Override
    public void setItem(int index, @Nullable ItemStack item) {
        inventory.setItem(index, item);
    }

    @Override
    public @NotNull HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return inventory.addItem(items);
    }

    @Override
    public @NotNull HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) throws IllegalArgumentException {
        return inventory.removeItem(items);
    }

    @Override
    public @NotNull HashMap<Integer, ItemStack> removeItemAnySlot(@NotNull ItemStack... items) throws IllegalArgumentException {
        return inventory.removeItemAnySlot(items);
    }

    @Override
    public @org.checkerframework.checker.nullness.qual.Nullable ItemStack @NonNull [] getContents() {
        return inventory.getContents();
    }

    @Override
    public void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        inventory.setContents(items);
    }

    @Override
    public @NotNull ItemStack[] getStorageContents() {
        return inventory.getStorageContents();
    }

    @Override
    public void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException {
        inventory.setStorageContents(items);
    }

    @Override
    public boolean contains(@NotNull Material material) throws IllegalArgumentException {
        return inventory.contains(material);
    }

    @Override
    public boolean contains(@Nullable ItemStack item) {
        return inventory.contains(item);
    }

    @Override
    public boolean contains(@NotNull Material material, int amount) throws IllegalArgumentException {
        return inventory.contains(material, amount);
    }

    @Override
    public boolean contains(@Nullable ItemStack item, int amount) {
        return inventory.contains(item, amount);
    }

    @Override
    public boolean containsAtLeast(@Nullable ItemStack item, int amount) {
        return inventory.containsAtLeast(item, amount);
    }

    @Override
    public @NotNull HashMap<Integer, ? extends ItemStack> all(@NotNull Material material) throws IllegalArgumentException {
        return inventory.all(material);
    }

    @Override
    public @NotNull HashMap<Integer, ? extends ItemStack> all(@Nullable ItemStack item) {
        return inventory.all(item);
    }

    @Override
    public int first(@NotNull Material material) throws IllegalArgumentException {
        return inventory.first(material);
    }

    @Override
    public int first(@NotNull ItemStack item) {
        return inventory.first(item);
    }

    @Override
    public int firstEmpty() {
        return inventory.firstEmpty();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public void remove(@NotNull Material material) throws IllegalArgumentException {
        inventory.remove(material);
    }

    @Override
    public void remove(@NotNull ItemStack item) {
        inventory.remove(item);
    }

    @Override
    public void clear(int index) {
        inventory.clear(index);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public int close() {
        return inventory.close();
    }

    @Override
    public @NotNull List<HumanEntity> getViewers() {
        return inventory.getViewers();
    }

    @Override
    public @NotNull InventoryType getType() {
        return inventory.getType();
    }

    @Override
    public @Nullable InventoryHolder getHolder() {
        return inventory.getHolder();
    }

    @Override
    public @Nullable InventoryHolder getHolder(boolean useSnapshot) {
        return inventory.getHolder(useSnapshot);
    }

    @Override
    public @NotNull ListIterator<ItemStack> iterator() {
        return inventory.iterator();
    }

    @Override
    public @NotNull ListIterator<ItemStack> iterator(int index) {
        return inventory.iterator(index);
    }

    @Override
    public @Nullable Location getLocation() {
        return location.clone();
    }
}
