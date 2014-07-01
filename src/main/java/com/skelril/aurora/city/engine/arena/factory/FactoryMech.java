/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.city.engine.arena.AbstractRegionedArena;
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
