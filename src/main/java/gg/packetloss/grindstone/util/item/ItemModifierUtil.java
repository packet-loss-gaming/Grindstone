/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemModifierUtil {
    public static class BaseModifier {
        private final Attribute baseAttribute;
        private final UUID modifierID;

        private BaseModifier(Attribute baseAttribute, UUID modifierID) {
            this.baseAttribute = baseAttribute;
            this.modifierID = modifierID;
        }

        public Attribute getBaseAttribute() {
            return baseAttribute;
        }

        public UUID getModifierID() {
            return this.modifierID;
        }

        public String getModifierName() {
            return this.baseAttribute.getKey().value();
        }

        public Modifier get(double value, AttributeModifier.Operation operation, @Nullable EquipmentSlot slot) {
            return new Modifier(this, value, operation, slot);
        }
    }

    public static final BaseModifier ITEM_ATTACK_DAMAGE = new BaseModifier(Attribute.GENERIC_ATTACK_DAMAGE, UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"));
    public static final BaseModifier ITEM_ATTACK_SPEED = new BaseModifier(Attribute.GENERIC_ATTACK_SPEED, UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"));

    public static class Modifier {
        private final BaseModifier modifier;
        private final double value;
        private final AttributeModifier.Operation operation;
        private final EquipmentSlot slot;

        private Modifier(BaseModifier modifier, double value, AttributeModifier.Operation operation, @Nullable EquipmentSlot slot) {
            this.modifier = modifier;
            this.value = value;
            this.operation = operation;
            this.slot = slot;
        }

        public Attribute getBaseAttribute() {
            return modifier.getBaseAttribute();
        }

        public UUID getModifierID() {
            return this.modifier.getModifierID();
        }

        public String getModifierName() {
            return this.modifier.getModifierName();
        }

        public double getValue() {
            return this.value;
        }

        public AttributeModifier.Operation getOperation() {
            return this.operation;
        }

        public EquipmentSlot getSlot() {
            return this.slot;
        }
    }

    public static ItemStack cloneWithSpecifiedModifiers(ItemStack baseStack, List<Modifier> modifiers) {
        ItemStack stack = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = stack.getItemMeta();
        for (Modifier modifier : modifiers) {
            meta.addAttributeModifier(
                modifier.getBaseAttribute(),
                new AttributeModifier(
                    modifier.getModifierID(),
                    modifier.getModifierName(),
                    modifier.getValue(),
                    modifier.getOperation(),
                    modifier.getSlot()
                )
            );
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
