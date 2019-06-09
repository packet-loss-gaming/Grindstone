/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CustomItem {
    private final CustomItems item;
    private final ItemStack base;
    private List<Tag> tags = new ArrayList<>();
    private List<String> lore = new ArrayList<>();
    private List<Enchant> enchants = new ArrayList<>();

    private List<String> useDocs = new ArrayList<>();
    private List<ItemSource> sources = new ArrayList<>();

    public CustomItem(CustomItems item, ItemStack base) {
        this.item = item;
        this.base = base;
    }

    public CustomItem(CustomItems item, Material type) {
        this(item, new ItemStack(type));
    }

    public CustomItems getItem() {
        return item;
    }

    public Material getBaseMaterial() {
        return base.getType();
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void addTag(ChatColor color, String key, String prop) {
        addTag(new Tag(color, key, prop));
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void addLore(String line) {
        lore.add(line);
    }

    public List<String> getLore() {
        return lore;
    }

    public void addEnchant(Enchant enchant) {
        enchants.add(enchant);
    }

    public void addEnchant(Enchantment enchant, int level) {
        addEnchant(new Enchant(enchant, level));
    }

    public List<Enchant> getEnchants() {
        return enchants;
    }

    public void addUse(String use) {
        useDocs.add(use);
    }

    public List<String> getUseDocs() {
        return useDocs;
    }

    public void addSource(ItemSource source) {
        sources.add(source);
    }

    public List<ItemSource> getSources() {
        List<ItemSource> sources = new ArrayList<>();
        for (ItemSource source : this.sources) {
            Collections.addAll(sources, source.getSubSources());
            sources.add(source);
        }
        return sources;
    }


    public ItemStack build() {
        ItemStack itemStack = base.clone();
        ItemMeta meta = itemStack.getItemMeta();
        for (Enchant enchant : enchants) {
            meta.addEnchant(enchant.getEnchant(), enchant.getLevel(), true);
        }
        List<String> lore = tags.stream().map(e -> e.getColor() + e.getKey() + ": " + e.getProp()).collect(Collectors.toList());
        lore.addAll(this.lore);
        if (!lore.isEmpty()) meta.setLore(lore);
        meta.setDisplayName(item.toString());
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
