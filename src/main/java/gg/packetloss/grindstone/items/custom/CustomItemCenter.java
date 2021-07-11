/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import com.google.gson.reflect.TypeToken;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

public class CustomItemCenter {
    public static final int REVISION = 0;

    private static CustomItemModelState modelState = new CustomItemModelState();
    private static SingleFileFilesystemStateHelper<CustomItemModelState> modelStateHelper;

    static  {
        try {
            modelStateHelper = new SingleFileFilesystemStateHelper<>("custom-item-models.json", new TypeToken<>() { });
            modelStateHelper.load().ifPresent(loadedState -> modelState = loadedState);

            if (modelState.update()) {
                modelStateHelper.save(modelState);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This should really only be used for migrating items
     */
    @Deprecated
    public static Optional<Integer> getModelId(ItemStack item) {
        return ItemNameCalculator.computeItemName(item).map(name -> modelState.itemNameToModelId.get(name));
    }

    public static int getModelId(CustomItems item) {
        return modelState.itemToModelId.get(item);
    }

    private static HashMap<CustomItems, CustomItem> items = new HashMap<>();

    private static void addItem(CustomItem item) {
        items.put(item.getItem(), item);
    }

    static {
        CustomItemCenterRegistration.register(CustomItemCenter::addItem);
    }

    public static Collection<CustomItem> values() {
        return items.values();
    }

    public static CustomItem get(CustomItems item) {
        CustomItemCloneVisitor visitor = new CustomItemCloneVisitor();
        items.get(item).accept(visitor);
        return visitor.out();
    }

    public static ItemStack build(CustomItems item) {
        return items.get(item).build();
    }

    public static ItemStack build(CustomItems item, int amt) {
        ItemStack stack = items.get(item).build();
        if (amt > stack.getMaxStackSize()) throw new IllegalArgumentException();
        stack.setAmount(amt);
        return stack;
    }
}
