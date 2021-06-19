/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.util.item.ItemModifierUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class CustomWeapon extends CustomEquipment {
    private static final double NO_SPEED_MOD = -1;

    private final double damageMod;
    private final double speedMod;

    public CustomWeapon(CustomItems item, Material type, double damageMod) {
        super(item, type);
        this.damageMod = damageMod;
        this.speedMod = NO_SPEED_MOD;
        addTag(ChatColor.RED, "Damage Modifier", String.valueOf(damageMod));
    }

    public CustomWeapon(CustomItems item, Material type, double damageMod, double speedMod) {
        super(item, type);
        this.damageMod = damageMod;
        this.speedMod = speedMod;
        addTag(ChatColor.RED, "Damage Modifier", String.valueOf(damageMod));
    }

    public CustomWeapon(CustomWeapon item) {
        super(item);
        this.damageMod = item.getDamageMod();
        this.speedMod = item.getSpeedMod().orElse(NO_SPEED_MOD);
    }

    public double getDamageMod() {
        return damageMod;
    }

    public boolean hasSpeedMod() {
        return speedMod != NO_SPEED_MOD;
    }

    public Optional<Double> getSpeedMod() {
        return hasSpeedMod() ? Optional.of(speedMod) : Optional.empty();
    }

    private int getDefaultDamage() {
        switch (getBaseType()) {
            case WOODEN_SWORD:
            case GOLDEN_SWORD:
                return 4;
            case STONE_SWORD:
                return 5;
            case IRON_SWORD:
                return 6;
            case DIAMOND_SWORD:
                return 7;
            case NETHERITE_SWORD:
                return 8;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void accept(CustomItemVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ItemStack build() {
        ItemStack stack = super.build();

        if (hasSpeedMod()) {
            stack = ItemModifierUtil.cloneWithSpecifiedModifiers(
                new ItemStack(Material.IRON_AXE),
                Lists.newArrayList(
                    ItemModifierUtil.ITEM_ATTACK_DAMAGE.get(
                        getDefaultDamage(),
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlot.HAND
                    ),
                    ItemModifierUtil.ITEM_ATTACK_SPEED.get(
                        speedMod,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlot.HAND
                    )
                )
            );
        }

        return stack;
    }
}
