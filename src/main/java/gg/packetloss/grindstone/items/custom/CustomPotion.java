/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public class CustomPotion extends CustomItem {
    private Color color;
    private List<Potion> effects = new ArrayList<>();

    public CustomPotion(CustomItems item, Color color) {
        super(item, Material.POTION);
        this.color = color;
    }

    public CustomPotion(CustomPotion potion) {
        super(potion);
        this.color = potion.getColor();
        effects.addAll(potion.getEffects());
    }

    public void addEffect(Potion effect) {
        effects.add(effect);
    }

    public void addEffect(PotionEffectType type, int time, int level) {
        addEffect(new Potion(type, time, level));
    }

    public Color getColor() {
        return color;
    }

    public List<Potion> getEffects() {
        return effects;
    }

    @Override
    public void accept(CustomItemVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ItemStack build() {
        ItemStack base = super.build();
        PotionMeta meta = (PotionMeta) base.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.UNCRAFTABLE));
        meta.setColor(color);
        for (Potion potion : effects) {
            meta.addCustomEffect(new PotionEffect(potion.getType(), potion.getTime(), potion.getLevel()), false);
        }
        base.setItemMeta(meta);
        return base;
    }
}
