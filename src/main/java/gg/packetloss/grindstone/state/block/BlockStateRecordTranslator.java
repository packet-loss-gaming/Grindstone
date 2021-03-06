/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.block;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BlockStateRecordTranslator {
    private BlockStateRecordTranslator() { }

    private static BlockStateRecord constructFrom(@Nullable UUID ownerID, BlockState blockState) {
        String blockData = blockState.getBlockData().getAsString();

        String worldName = blockState.getWorld().getName();
        int x = blockState.getX();
        int y = blockState.getY();
        int z = blockState.getZ();

        return new BlockStateRecord(ownerID, blockData, worldName, x, y, z);
    }

    public static BlockStateRecord constructFrom(Player player, BlockState blockState) {
        UUID playerID = player.getUniqueId();
        return constructFrom(playerID, blockState);
    }

    public static BlockStateRecord constructFrom(BlockState blockState) {
        return constructFrom((UUID) null, blockState);
    }

    public static CompletableFuture<Boolean> restore(BlockStateRecord record) {
        World world = Bukkit.getWorld(record.getWorldName());

        return world.getChunkAtAsync(record.getChunkX(), record.getChunkZ()).thenApply((chunk) -> {
            Block block = world.getBlockAt(record.getX(), record.getY(), record.getZ());
            block.setBlockData(Bukkit.createBlockData(record.getBlockData()));
            return true;
        });
    }
}
