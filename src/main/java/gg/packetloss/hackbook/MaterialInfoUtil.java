package gg.packetloss.hackbook;

import net.minecraft.server.v1_16_R1.ItemTool;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;

public class MaterialInfoUtil {
    public static float getBreakSpeed(Material tool, Block block) {
        ItemTool item = (ItemTool) CraftMagicNumbers.getItem(tool);
        return item.getDestroySpeed(CraftItemStack.asNMSCopy(new ItemStack(tool)), ((CraftBlock) block).getNMS());
    }
}
