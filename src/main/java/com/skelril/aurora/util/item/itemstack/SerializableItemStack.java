package com.skelril.aurora.util.item.itemstack;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Wyatt Childers
 * Date: 10/11/13
 */
public class SerializableItemStack implements Serializable {

    Map<String, Object> map;


    public SerializableItemStack(ItemStack itemStack) {

        map = itemStack.serialize();
        if (map.containsKey("meta")) {
            map.put("meta", itemStack.getItemMeta().serialize());
        }
    }

    public ItemStack bukkitRestore() {

        ItemStack stack = ItemStack.deserialize(map);
        ItemMeta meta = null;
        if (map.containsKey("meta")) {
            Object metaMap = map.get("meta");
            if (metaMap instanceof Map) {
                Map<String, Object> aMetaMap = new HashMap<>();
                aMetaMap.put("==", "ItemMeta");
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) metaMap).entrySet()) {
                    aMetaMap.put(entry.getKey(), entry.getValue());
                }
                meta = (ItemMeta) ConfigurationSerialization.deserializeObject(aMetaMap);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
