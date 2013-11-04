package com.skelril.aurora.util.item.itemstack;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            Map<String, Object> aMetaMap = new HashMap<>();
            aMetaMap.put("==", ConfigurationSerialization.getAlias(itemStack.getItemMeta().getClass()));
            for (Map.Entry<String, Object> entry : itemStack.getItemMeta().serialize().entrySet()) {
                /* Object deathMachine = ;

                System.out.println(entry.getKey() + ":" + entry.getValue().getClass().getName());
                if (deathMachine instanceof ConfigurationSerializable) {
                    Map<String, Object> deathMap = new HashMap<>();
                    deathMap.put("==", ConfigurationSerialization.getAlias(itemStack.getItemMeta().getClass()));
                    for (Map.Entry<String, Object> aEntry : ((ConfigurationSerializable) deathMachine).serialize().entrySet()) {
                        deathMap.put(aEntry.getKey(), aEntry.getValue());
                        System.out.println(aEntry.getKey() + ":" + aEntry.getValue().getClass().getName());
                    }
                    deathMachine = deathMap;
                }
                */

                aMetaMap.put(entry.getKey(), entry.getValue());
            }

            if (aMetaMap.containsKey("custom-effects")) {
                List<Map<String, Object>> potionEffects = new ArrayList<>();
                for (PotionEffect effect : (List<PotionEffect>) aMetaMap.get("custom-effects")) {
                    Map<String, Object> aMap = new HashMap<>();
                    aMap.put("==", ConfigurationSerialization.getAlias(effect.getClass()));

                    for (Map.Entry<String, Object> entry : effect.serialize().entrySet()) {

                        aMap.put(entry.getKey(), entry.getValue());
                    }
                    potionEffects.add(aMap);
                }
                aMetaMap.put("custom-effects", potionEffects);
            }
            map.put("meta", aMetaMap);
        }
    }

    public ItemStack bukkitRestore() {

        ItemStack stack = ItemStack.deserialize(map);
        ItemMeta meta = null;
        if (map.containsKey("meta")) {
            Object metaMap = map.get("meta");
            if (metaMap instanceof Map) {
                Map<String, Object> aMetaMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) metaMap).entrySet()) {
                    aMetaMap.put(entry.getKey(), entry.getValue());
                }

                if (aMetaMap.containsKey("custom-effects")) {
                    List<PotionEffect> potionEffects = new ArrayList<>();
                    for (Map<String, Object> entry : (List<Map<String, Object>>) aMetaMap.get("custom-effects")) {
                        potionEffects.add((PotionEffect) ConfigurationSerialization.deserializeObject(entry));
                    }
                    aMetaMap.put("custom-effects", potionEffects);
                }

                meta = (ItemMeta) ConfigurationSerialization.deserializeObject(aMetaMap);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
