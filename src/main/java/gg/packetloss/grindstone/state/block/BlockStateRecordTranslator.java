package gg.packetloss.grindstone.state.block;

import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class BlockStateRecordTranslator {
    private BlockStateRecordTranslator() { }

    private static BlockStateRecord constructFrom(@Nullable UUID ownerID, BlockState blockState) throws UnstorableBlockStateException {
        String blockType = ItemNameCalculator.computeBlockName(
                blockState.getTypeId(), blockState.getRawData()
        ).orElseThrow(UnstorableBlockStateException::new);

        String worldName = blockState.getWorld().getName();
        int x = blockState.getX();
        int y = blockState.getY();
        int z = blockState.getZ();

        return new BlockStateRecord(ownerID, blockType, worldName, x, y, z);
    }

    public static BlockStateRecord constructFrom(Player player, BlockState blockState) throws UnstorableBlockStateException {
        UUID playerID = player.getUniqueId();
        return constructFrom(playerID, blockState);
    }

    public static BlockStateRecord constructFrom(BlockState blockState) throws UnstorableBlockStateException {
        return constructFrom((UUID) null, blockState);
    }

    public static void restore(BlockStateRecord record) {
        World world = Bukkit.getWorld(record.getWorldName());

        ItemNameCalculator.NumericItem item = ItemNameCalculator.toNumeric(record.getBlockType()).orElseThrow(IllegalStateException::new);

        world.getBlockAt(record.getX(), record.getY(), record.getZ()).setTypeIdAndData(item.getId(), (byte) item.getData(), false);
    }
}
