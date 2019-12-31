package gg.packetloss.hackbook;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.Dynamic;
import net.minecraft.server.v1_13_R2.DataConverterTypes;
import net.minecraft.server.v1_13_R2.DynamicOpsNBT;
import net.minecraft.server.v1_13_R2.MinecraftServer;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;

class DataMigrator {
    // IMPORTANT: These must be updated every minecraft version.
    private static final int PREV_VERSION = 1343;
    private static final int NEW_VERSION = 1631;

    private static NBTTagCompound runFixer(DSL.TypeReference typeReference, NBTTagCompound tag) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        DataFixer fixer = server.dataConverterManager;

        int prevVersion = tag.hasKey("DataVersion") ? tag.getInt("DataVersion") : PREV_VERSION;

        return (NBTTagCompound) fixer.update(
                typeReference,
                new Dynamic<>(DynamicOpsNBT.a, tag),
                prevVersion,
                NEW_VERSION
        ).getValue();
    }

    public static NBTTagCompound updateItemStack(NBTTagCompound tag) {
        return runFixer(DataConverterTypes.ITEM_STACK, tag);
    }

    public static int getCurrentVersion() {
        return NEW_VERSION;
    }
}
