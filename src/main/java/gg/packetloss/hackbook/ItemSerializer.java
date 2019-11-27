package gg.packetloss.hackbook;

import net.minecraft.server.v1_12_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemSerializer {
    private static NBTTagCompound toTag(ItemStack stack) {
        NBTTagCompound compound = new NBTTagCompound();
        CraftItemStack.asNMSCopy(stack).save(compound);
        return compound;
    }

    public static String toJSON(ItemStack stack) {
        return toTag(stack).toString();
    }

    public static void writeToOutputStream(ItemStack stack, OutputStream stream) throws IOException {
        NBTCompressedStreamTools.a(toTag(stack), stream);
    }

    public static void writeToOutputStream(Collection<ItemStack> stacks, OutputStream stream) throws IOException {
        NBTTagCompound compoundTag = new NBTTagCompound();

        NBTTagList tag = new NBTTagList();
        for (ItemStack stack : stacks) {
            tag.add(toTag(stack));
        }

        compoundTag.set("elements", tag);

        NBTCompressedStreamTools.a(compoundTag, stream);
    }

    public static List<ItemStack> fromInputStream(InputStream stream) throws IOException {
        NBTTagCompound compoundTag = NBTCompressedStreamTools.a(stream);

        NBTTagList tag = (NBTTagList) compoundTag.get("elements");

        List<ItemStack> stacks = new ArrayList<>(tag.size());

        for (int i = 0; i < tag.size(); ++i) {
            NBTTagCompound itemTag = tag.get(i);
            stacks.add(CraftItemStack.asCraftMirror(new net.minecraft.server.v1_12_R1.ItemStack(itemTag)));
        }

        return stacks;
    }
}
