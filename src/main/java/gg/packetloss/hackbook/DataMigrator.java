package gg.packetloss.hackbook;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.Dynamic;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;

class DataMigrator {
    private static NBTTagCompound runFixer(int prevVersion, DSL.TypeReference typeReference, NBTTagCompound tag) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        DataFixer fixer = server.dataConverterManager;

        return (NBTTagCompound) fixer.update(
                typeReference,
                new Dynamic<>(DynamicOpsNBT.a, tag),
                prevVersion,
                getCurrentVersion()
        ).getValue();
    }

    public static NBTTagCompound updateItemStack(int prevVersion, NBTTagCompound tag) {
        return runFixer(prevVersion, DataConverterTypes.ITEM_STACK, tag);
    }

    public static int getCurrentVersion() {
        return SharedConstants.a().getWorldVersion();
    }
}
