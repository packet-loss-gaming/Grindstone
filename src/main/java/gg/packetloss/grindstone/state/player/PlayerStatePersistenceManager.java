package gg.packetloss.grindstone.state.player;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatePersistenceManager {
    private Map<UUID, List<ItemStack>> inventoryCache = new ConcurrentHashMap<>();
    private NativeSerializerComponent serializer;

    public PlayerStatePersistenceManager(NativeSerializerComponent serializer) {
        this.serializer = serializer;
    }

    public void loadInventory(UUID inventoryID) throws IOException {
        inventoryCache.put(inventoryID, serializer.readItems(inventoryID));
    }

    public void unloadInventory(UUID inventoryID) {
        inventoryCache.remove(inventoryID);
    }

    public void addInventory(UUID inventoryID, Player player) throws IOException  {
        List<ItemStack> stacks = Lists.newArrayList(ItemUtil.clone(player.getInventory().getContents()));
        inventoryCache.put(inventoryID, stacks);

        serializer.writeItems(inventoryID, stacks);
    }

    public List<ItemStack> removeInventory(UUID inventoryID) throws IOException {
        List<ItemStack> contents = inventoryCache.remove(inventoryID);
        serializer.removeItems(inventoryID);
        return contents;
    }
}
