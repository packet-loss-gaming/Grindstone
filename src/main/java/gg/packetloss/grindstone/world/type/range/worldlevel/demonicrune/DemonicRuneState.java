/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.demonicrune;

import gg.packetloss.grindstone.util.RomanNumeralUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DemonicRuneState {
    private final int worldTier;
    private final int monsterTier;
    private final int combatTier;

    public DemonicRuneState(int worldTier, int monsterTier, int combatTier) {
        this.worldTier = worldTier;
        this.monsterTier = monsterTier;
        this.combatTier = combatTier;
    }

    public DemonicRuneState() {
        this(-1, -1, -1);
    }

    public static DemonicRuneState fromMonsterKill(int worldTier, double monsterTypeModifier, double percentDamageDone) {
        return new DemonicRuneState(
            worldTier,
            (int) (monsterTypeModifier * 100),
            (int) (percentDamageDone * 100)
        );
    }

    public int getWorldLevel() {
        return worldTier;
    }

    public double getTypeModifier() {
        return monsterTier / 100D;
    }

    public double getPercentDamageDone() {
        return combatTier / 100D;
    }

    public static void makeItemFromRuneState(ItemStack itemStack, DemonicRuneState runeState) {
        List<Map.Entry<String, String>> worldTier = new ArrayList<>();
        worldTier.add(new AbstractMap.SimpleEntry<>("World Tier", RomanNumeralUtil.toRoman(runeState.worldTier)));

        List<Map.Entry<String, String>> monsterTier = new ArrayList<>();
        monsterTier.add(new AbstractMap.SimpleEntry<>("Monster Tier", RomanNumeralUtil.toRoman(runeState.monsterTier)));

        List<Map.Entry<String, String>> combatTier = new ArrayList<>();
        combatTier.add(new AbstractMap.SimpleEntry<>("Combat Tier", RomanNumeralUtil.toRoman(runeState.combatTier)));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(List.of(
            ItemUtil.saveLoreKeyValues(worldTier),
            ItemUtil.saveLoreKeyValues(monsterTier),
            ItemUtil.saveLoreKeyValues(combatTier)
        ));
        itemStack.setItemMeta(itemMeta);
    }

    public static DemonicRuneState getRuneStateFromItem(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.isEmpty()) {
            return new DemonicRuneState();
        }

        List<Map.Entry<String, String>> worldTierValues = ItemUtil.loadLoreKeyValues(lore.get(0));
        String worldTierStr = worldTierValues.get(0).getValue();
        if (worldTierStr == null) {
            return new DemonicRuneState();
        }

        List<Map.Entry<String, String>> monsterTierValues = ItemUtil.loadLoreKeyValues(lore.get(1));
        String monsterTierStr = monsterTierValues.get(0).getValue();
        if (monsterTierStr == null) {
            return new DemonicRuneState();
        }

        List<Map.Entry<String, String>> combatTierValues = ItemUtil.loadLoreKeyValues(lore.get(2));
        String combatTierStr = combatTierValues.get(0).getValue();
        if (combatTierStr == null) {
            return new DemonicRuneState();
        }

        int worldTier = RomanNumeralUtil.fromRoman(worldTierStr);
        int monsterTier = RomanNumeralUtil.fromRoman(monsterTierStr);
        int combatTier = RomanNumeralUtil.fromRoman(combatTierStr);

        return new DemonicRuneState(worldTier, monsterTier, combatTier);
    }

}
