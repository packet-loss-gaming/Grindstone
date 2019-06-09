/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import com.google.common.collect.Lists;
import gg.packetloss.hackbook.ModifierBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class CustomWeapon extends CustomEquipment {

    private final double damageMod;
    private final double speedMod;

    public CustomWeapon(CustomItems item, Material type, double damageMod) {
        super(item, type);
        this.damageMod = damageMod;
        this.speedMod = -1;
        addTag(ChatColor.RED, "Damage Modifier", String.valueOf(damageMod));
    }

    public CustomWeapon(CustomItems item, Material type, double damageMod, double speedMod) {
        super(item, type);
        this.damageMod = damageMod;
        this.speedMod = speedMod;
        addTag(ChatColor.RED, "Damage Modifier", String.valueOf(damageMod));
    }

    public double getDamageMod() {
        return damageMod;
    }

    public boolean hasSpeedMod() {
        return getSpeedMod().isPresent();
    }

    public Optional<Double> getSpeedMod() {
        return speedMod == -1 ? Optional.empty() : Optional.of(speedMod);
    }

    private int getDefaultDamage() {
        switch (getBaseMaterial()) {
            case WOOD_SWORD:
            case GOLD_SWORD:
                return 4;
            case STONE_SWORD:
                return 5;
            case IRON_SWORD:
                return 6;
            case DIAMOND_SWORD:
                return 7;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public ItemStack build() {
        ItemStack stack = super.build();

        if (hasSpeedMod()) {
            try {
                stack = ModifierBook.cloneWithSpecifiedModifiers(
                        stack,
                        Lists.newArrayList(
                                ModifierBook.ITEM_ATTACK_DAMAGE.get(
                                        getDefaultDamage(),
                                        ModifierBook.ModifierOperation.ADDITIVE,
                                        ModifierBook.Slot.MAIN_HAND
                                ),
                                ModifierBook.ITEM_ATTACK_SPEED.get(
                                        speedMod,
                                        ModifierBook.ModifierOperation.ADDITIVE,
                                        ModifierBook.Slot.MAIN_HAND
                                )
                        )
                );
            } catch (UnsupportedFeatureException e) {
                return stack;
            }
        }

        return stack;
    }
}
