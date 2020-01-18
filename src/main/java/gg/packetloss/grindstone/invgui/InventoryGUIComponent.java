package gg.packetloss.grindstone.invgui;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@ComponentInformation(friendlyName = "Inventory GUI", desc = "Create GUIs using inventories")
public class InventoryGUIComponent extends BukkitComponent implements Listener {
    private Map<Inventory, Consumer<Inventory>> closeCallbacks = new HashMap<>();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    public Inventory openClosableChest(Player player, String title, Consumer<Inventory> closeCallback) {
           Inventory inv = Bukkit.createInventory(player, 27, title);
           closeCallbacks.put(inv, closeCallback);
           player.openInventory(inv);
           return inv;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Consumer<Inventory> closeCallback = closeCallbacks.get(event.getInventory());
        if (closeCallback == null) {
            return;
        }

        closeCallback.accept(event.getInventory());
    }
}
