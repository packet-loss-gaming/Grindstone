/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration.migrations;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.migration.Migration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class OPCombatPotionNerfMigration implements Migration {
    private static final List<PotionEffectType> EFFECT_TYPES = List.of(
        PotionEffectType.STRENGTH, PotionEffectType.REGENERATION,
        PotionEffectType.RESISTANCE, PotionEffectType.WATER_BREATHING,
        PotionEffectType.FIRE_RESISTANCE
    );

    private static final List<Integer> EFFECT_LEVELS = List.of(
        3, 2
    );

    private boolean isPartOfCombatPotion(PotionEffect effect) {
        return EFFECT_TYPES.contains(effect.getType());
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack.getType() != Material.POTION) {
            return false;
        }

        PotionMeta itemMeta = (PotionMeta) itemStack.getItemMeta();

        // Check the effect counts
        List<PotionEffect> effects = Lists.newArrayList(itemMeta.getCustomEffects());
        if (effects.size() != EFFECT_TYPES.size()) {
            return false;
        }

        // Find the amplified and make sure it's the same at all levels
        int amplifier = -1;
        for (PotionEffect effect : effects) {
            // Set the amplifier if we haven't seen it yet
            if (amplifier == -1) {
                amplifier = effect.getAmplifier();
            }

            if (amplifier != effect.getAmplifier()) {
                return false;
            }
        }

        // Check that the amplifier matches expectations.
        if (!EFFECT_LEVELS.contains(amplifier)) {
            return false;
        }

        // Check that all effects are effects we expect at levels we expect
        return effects.stream().allMatch(this::isPartOfCombatPotion);
    }

    @Override
    public ItemStack apply(ItemStack itemStack) {
        PotionMeta itemMeta = (PotionMeta) itemStack.getItemMeta();
        List<PotionEffect> effects = itemMeta.getCustomEffects();

        int amplifier = -1;
        int duration = -1;
        for (PotionEffect effect : effects) {
            // Set the amplifier if we haven't seen it yet
            if (amplifier == -1) {
                amplifier = effect.getAmplifier();
                duration = effect.getDuration();
            }
        }

        if (amplifier == 3) {
            if (duration == 20 * 600) {
                return CustomItemCenter.build(CustomItems.DIVINE_COMBAT_POTION);
            } else {
                return CustomItemCenter.build(CustomItems.HOLY_COMBAT_POTION);
            }
        } else{
            return CustomItemCenter.build(CustomItems.EXTREME_COMBAT_POTION);
        }
    }
}

