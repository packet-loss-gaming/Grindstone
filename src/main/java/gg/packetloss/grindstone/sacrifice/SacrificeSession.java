/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import com.sk89q.commandbook.component.session.PersistentSession;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.state.player.NativeSerializerComponent;
import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

// Sacrifice Session
class SacrificeSession extends PersistentSession {
    @Setting("inventory.id")
    private String inventoryID = null;
    @Setting("inventory.length")
    private int size = 0;

    private List<ItemStack> loadedInventory = null;

    protected SacrificeSession() {
        super(-1L);
    }

    private static NativeSerializerComponent getNativeSerializer() {
        return SacrificeComponent.getNativeSerializer();
    }

    public Player getPlayer() {
        CommandSender sender = super.getOwner();
        return sender instanceof Player ? (Player) sender : null;
    }

    private void loadNewInventory() {
        Validate.isTrue(inventoryID == null);
        Validate.isTrue(loadedInventory == null);

        loadedInventory = new ArrayList<>();
    }

    private void loadExistingInventory() {
        Validate.notNull(inventoryID);
        Validate.isTrue(loadedInventory == null);

        UUID originalID = UUID.fromString(inventoryID);

        try {
            loadedInventory = getNativeSerializer().readItems(originalID);

            // Clear the saved copy
            getNativeSerializer().removeItems(originalID);
            inventoryID = null;
        } catch (IOException e) {
            e.printStackTrace();

            // Restore the original state
            inventoryID = originalID.toString();
            loadedInventory = null;

            throw new RuntimeException(e);
        }
    }

    private void loadItems() {
        if (size < 1) {
            loadNewInventory();
            return;
        }

        loadExistingInventory();
    }

    private void unloadItems() {
        Validate.isTrue(inventoryID == null);
        Validate.notNull(loadedInventory);

        List<ItemStack> originalInventory = loadedInventory;

        try {
            // Write the on disk copy
            if (size > 0) {
                UUID newID = UUID.randomUUID();
                inventoryID = newID.toString();
                getNativeSerializer().writeItems(newID, loadedInventory);
            }

            // Clear the loaded copy
            loadedInventory = null;
        } catch (IOException e) {
            e.printStackTrace();

            // Restore the original state
            inventoryID = null;
            loadedInventory = originalInventory;

            throw new RuntimeException(e);
        }
    }

    private void lazyLoad(Consumer<List<ItemStack>> consumer) {
        boolean wasAlreadyLoaded = loadedInventory != null;
        if (!wasAlreadyLoaded) {
            loadItems();
        }

        try {
            consumer.accept(loadedInventory);
        } finally {
            if (!wasAlreadyLoaded) {
                unloadItems();
            }
        }
    }

    public void addItems(List<ItemStack> itemStacks) {
        lazyLoad((queue) -> {
            queue.addAll(itemStacks);
            size = queue.size();
        });
    }

    public void addItem(ItemStack itemStack) {
        addItems(List.of(itemStack));
    }

    public void pollItems(Function<ItemStack, Boolean> consumer) {
        lazyLoad((queue) -> {
            while (!queue.isEmpty()) {
                ItemStack itemStack = queue.remove(queue.size() - 1);
                size = queue.size();

                if (consumer.apply(itemStack)) {
                    break;
                }
            }
        });
    }

    public boolean hasItems() {
        return size > 0;
    }

    public int remaining() {
        return size;
    }
}
