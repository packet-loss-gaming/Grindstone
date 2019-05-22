/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena.factory;

import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.city.engine.arena.AbstractRegionedArena;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FactoryMech extends AbstractRegionedArena {

  protected Map<Integer, Integer> items = new ConcurrentHashMap<>();
  protected YAMLProcessor processor;
  protected String data;

  public FactoryMech(World world, ProtectedRegion region, YAMLProcessor processor, String data) {
    super(world, region);
    this.processor = processor;
    this.data = data;
  }

  public abstract List<ItemStack> process();

  public void load() {
    try {
      processor.load();
      Map<String, YAMLNode> nodes = processor.getNodes(data);
      if (nodes != null) {
        for (Map.Entry<String, YAMLNode> entry : nodes.entrySet()) {
          YAMLNode node = entry.getValue();
          items.put(Integer.parseInt(entry.getKey()), node.getInt("amt"));
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void save() {
    processor.removeProperty(data);
    for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
      YAMLNode node = processor.addNode(data + '.' + entry.getKey());
      node.setProperty("amt", entry.getValue());
    }
    processor.save();
  }
}
