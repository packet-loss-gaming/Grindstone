package gg.packetloss.hackbook;

import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ModifierBook {
    public static class BaseModifier {
        private final UUID modifierID;
        private final String modifierName;

        private BaseModifier(UUID modifierID, String modifierName) {
            this.modifierID = modifierID;
            this.modifierName = modifierName;
        }

        public UUID getModifierID() {
            return modifierID;
        }

        public String getModifierName() {
            return modifierName;
        }


        public Modifier get(double value, ModifierOperation operation, @Nullable Slot slot) {
            return new Modifier(this, value, operation, slot);
        }
    }

    public enum ModifierOperation {
        ADDITIVE(0);

        private final int opCode;

        private ModifierOperation(int opCode) {
            this.opCode = opCode;
        }

        protected int getOpCode() {
            return opCode;
        }
    }

    public enum Slot {
        MAIN_HAND("mainhand"),
        OFF_HAND("offhand"),
        HEAD("head"),
        CHEST("chest"),
        LEGS("legs"),
        FEET("feet");

        private final String name;

        private Slot(String name) {
            this.name = name;
        }

        protected String getName() {
            return name;
        }
    }

    public static class Modifier {
        private final BaseModifier modifier;
        private final double value;
        private final ModifierOperation operation;
        private final Slot slot;

        private Modifier(BaseModifier modifier, double value, ModifierOperation operation, @Nullable Slot slot) {
            this.modifier = modifier;
            this.value = value;
            this.operation = operation;
            this.slot = slot;
        }

        public UUID getModifierID() {
            return modifier.getModifierID();
        }

        public String getModifierName() {
            return modifier.getModifierName();
        }

        public double getValue() {
            return value;
        }

        public ModifierOperation getOperation() {
            return operation;
        }

        public Slot getSlot() {
            return slot;
        }
    }

    public static final BaseModifier ITEM_ATTACK_DAMAGE = new BaseModifier(
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"), "generic.attackDamage"
    );
    public static final BaseModifier ITEM_ATTACK_SPEED = new BaseModifier(
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), "generic.attackSpeed"
    );

    private static NBTTagCompound buildModifierTag(Modifier modifier) {
        NBTTagCompound modifierTag = new NBTTagCompound();

        Slot slot = modifier.getSlot();
        if (slot != null) {
            modifierTag.set("Slot", new NBTTagString(slot.getName()));
        }

        modifierTag.set("AttributeName", new NBTTagString(modifier.getModifierName()));
        modifierTag.set("Name", new NBTTagString(modifier.getModifierName()));
        modifierTag.set("Amount", new NBTTagDouble(modifier.getValue()));
        modifierTag.set("Operation", new NBTTagInt(modifier.getOperation().getOpCode()));
        modifierTag.set("UUIDLeast", new NBTTagInt((int) modifier.getModifierID().getLeastSignificantBits()));
        modifierTag.set("UUIDMost", new NBTTagInt((int) modifier.getModifierID().getMostSignificantBits()));

        return modifierTag;
    }

    public static ItemStack cloneWithSpecifiedModifiers(ItemStack stack, List<Modifier> modifierList) throws UnsupportedFeatureException {
        try {
            net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);

            NBTTagCompound compound = nmsStack.getTag();
            if (compound == null) {
                nmsStack.setTag(new NBTTagCompound());
                compound = nmsStack.getTag();
            }

            NBTTagList modifiers = new NBTTagList();

            for (Modifier modifier : modifierList) {
                modifiers.add(buildModifierTag(modifier));
            }

            compound.set("AttributeModifiers", modifiers);

            return CraftItemStack.asBukkitCopy(nmsStack);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new UnsupportedFeatureException();
        }
    }

}
