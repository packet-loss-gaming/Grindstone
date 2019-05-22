/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.itemstack;

import org.bukkit.Color;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class StackSerializer {
  public static Map<String, Object> getMap(ItemStack itemStack) {
    Map<String, Object> map = itemStack.serialize();
    if (map.containsKey("meta")) {
      Map<String, Object> aMetaMap = new HashMap<>();
      aMetaMap.put("==", ConfigurationSerialization.getAlias(itemStack.getItemMeta().getClass()));
      for (Map.Entry<String, Object> entry : itemStack.getItemMeta().serialize().entrySet()) {

        String key = entry.getKey();
        Object o = entry.getValue();

        switch (key) {
          case "custom-effects":
            List<Map<String, Object>> potionEffects = new ArrayList<>();
            for (PotionEffect effect : (List<PotionEffect>) o) {
              Map<String, Object> aPotionEffectMap = new HashMap<>();
              aPotionEffectMap.put("==", ConfigurationSerialization.getAlias(effect.getClass()));
              for (Map.Entry<String, Object> aEntry : effect.serialize().entrySet()) {
                aPotionEffectMap.put(aEntry.getKey(), aEntry.getValue());
              }
              potionEffects.add(aPotionEffectMap);
            }
            o = potionEffects;
            break;
          case "color":
            Map<String, Object> aColorMap = new HashMap<>();
            //noinspection RedundantCast
            aColorMap.put("==", ConfigurationSerialization.getAlias(((Color) o).getClass()));
            for (Map.Entry<String, Object> aEntry : ((Color) o).serialize().entrySet()) {
              aColorMap.put(aEntry.getKey(), aEntry.getValue());
            }
            o = aColorMap;
            break;
        }

        ByteArrayOutputStream bos;
        ObjectOutputStream oss = null;
        try {
          bos = new ByteArrayOutputStream();
          oss = new ObjectOutputStream(bos);
          oss.writeObject(o);
          oss.close();
        } catch (IOException ex) {
          System.out.println("Error encountered processing key: " + key);
          ex.printStackTrace();
          continue;
        } finally {
          if (oss != null) {
            try {
              oss.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }

        aMetaMap.put(key, o);
      }
      map.put("meta", aMetaMap);
    }
    return map;
  }

  public static ItemStack fromMap(Map<String, Object> map) {
    ItemStack stack = ItemStack.deserialize(map);
    ItemMeta meta = null;
    if (map.containsKey("meta")) {
      Object metaMap = map.get("meta");
      if (metaMap instanceof Map) {
        Map<String, Object> aMetaMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) metaMap).entrySet()) {
          aMetaMap.put(entry.getKey(), entry.getValue());
        }

        // Maintain compatibility with older versions of the class
        if (!aMetaMap.containsKey("==")) {
          aMetaMap.put("==", "ItemMeta");
        }

        if (aMetaMap.containsKey("custom-effects")) {
          List<PotionEffect> potionEffects = ((List<Map<String, Object>>) aMetaMap.get("custom-effects"))
              .stream().map(entry -> (PotionEffect) ConfigurationSerialization.deserializeObject(entry))
              .collect(Collectors.toList());
          aMetaMap.put("custom-effects", potionEffects);
        }

        if (aMetaMap.containsKey("color")) {
          aMetaMap.put("color", ConfigurationSerialization.deserializeObject((Map<String, Object>) aMetaMap.get("color")));
        }

        meta = (ItemMeta) ConfigurationSerialization.deserializeObject(aMetaMap);
      }
      stack.setItemMeta(meta);
    }
    return stack;
  }
}
