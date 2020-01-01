package gg.packetloss.hackbook;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.Dynamic;
import net.minecraft.server.v1_14_R1.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;

class DataMigrator {
    private static NBTTagCompound runFixer(DSL.TypeReference typeReference, NBTTagCompound tag) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        DataFixer fixer = server.dataConverterManager;

        Validate.isTrue(tag.hasKey("DataVersion"));
        int prevVersion = tag.getInt("DataVersion") ;

        return (NBTTagCompound) fixer.update(
                typeReference,
                new Dynamic<>(DynamicOpsNBT.a, tag),
                prevVersion,
                getCurrentVersion()
        ).getValue();
    }

    public static NBTTagCompound updateItemStack(NBTTagCompound tag) {
        return runFixer(DataConverterTypes.ITEM_STACK, tag);
    }

    public static int getCurrentVersion() {
        return SharedConstants.a().getWorldVersion();
    }
}
