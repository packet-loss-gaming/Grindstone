package gg.packetloss.hackbook;

import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ItemSerializer {
    public static String toJSON(ItemStack stack) {
        NBTTagCompound compound = new NBTTagCompound();
        CraftItemStack.asNMSCopy(stack).save(compound);
        return compound.toString();
    }
}
