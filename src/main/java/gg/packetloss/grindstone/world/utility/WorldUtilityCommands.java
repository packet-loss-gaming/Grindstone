/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.utility;

import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Switch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class WorldUtilityCommands {
    private Set<String> pregenActiveSet = new HashSet<>();

    @Command(name = "pregen", desc = "Used to pregenerate all chunks within a world border")
    @CommandPermissions({"aurora.admin.worldutility.pregenerate"})
    public void pregenCmd(Player player, @Switch(name = 'y', desc = "yes") boolean confirm) {
        World world = player.getWorld();
        String worldName = world.getName();

        if (pregenActiveSet.contains(worldName)) {
            ChatUtil.sendError(player, "Already in progress.");
            return;
        }

        pregenActiveSet.add(worldName);

        WorldBorder worldBorder = world.getWorldBorder();
        Location center = worldBorder.getCenter();
        double size = worldBorder.getSize() / 2;

        CuboidRegion region = new CuboidRegion(
            BlockVector3.at(
                center.getBlockX() - size,
                world.getMinHeight(),
                center.getBlockZ() - size
            ),
            BlockVector3.at(
                center.getBlockX() + size,
                world.getMaxHeight(),
                center.getBlockZ() + size
            )
        );

        ChatUtil.sendNotice(player, "Minimum Boundary: " + region.getMinimumPoint());
        ChatUtil.sendNotice(player, "Maximum Boundary: " + region.getMaximumPoint());

        List<BlockVector2> fullChunkList = new ArrayList<>();
        RegionWalker.walkChunks(region, (x, z) -> {
            fullChunkList.add(BlockVector2.at(x, z));
        });

        Text noticePrefix = Text.of(
            Text.of(ChatColor.BLUE, worldName),
            " - "
        );

        ChatUtil.sendAdminNotice(
            ChatColor.YELLOW,
            noticePrefix,
            "Considering ",
            Text.of(ChatColor.WHITE, ChatUtil.WHOLE_NUMBER_FORMATTER.format(fullChunkList.size())),
            " chunks."
        );

        TaskBuilder.Countdown walkerTaskBuilder = TaskBuilder.countdown();

        List<BlockVector2> toGenChunkList = new ArrayList<>();
        walkerTaskBuilder.setAction((times) -> {
            long startTime = System.nanoTime();
            long maxTime = TimeUtil.convertTicksToNanos(1);

            while (!fullChunkList.isEmpty() && System.nanoTime() - startTime < maxTime) {
                BlockVector2 chunkCoords = fullChunkList.remove(fullChunkList.size() - 1);
                if (!world.isChunkGenerated(chunkCoords.getX(), chunkCoords.getZ())) {
                    toGenChunkList.add(chunkCoords);
                }
            }

            if (ChanceUtil.getChance(20 * 5)) {
                ChatUtil.sendAdminNotice(
                    ChatColor.YELLOW,
                    noticePrefix,
                    Text.of(ChatColor.WHITE, ChatUtil.WHOLE_NUMBER_FORMATTER.format(fullChunkList.size())),
                    " chunks left to consider."
                );
            }

            return fullChunkList.isEmpty();
        });

        walkerTaskBuilder.setFinishAction(() -> {
            if (toGenChunkList.isEmpty()) {
                ChatUtil.sendAdminNotice(
                    ChatColor.RED,
                    noticePrefix,
                    "All chunks within the world border have already been generated."
                );
                pregenActiveSet.remove(worldName);
                return;
            }

            if (!confirm) {
                ChatUtil.sendAdminNotice(
                    ChatColor.YELLOW,
                    noticePrefix,
                    Text.of(ChatColor.WHITE, ChatUtil.WHOLE_NUMBER_FORMATTER.format(toGenChunkList.size())),
                    " chunks would be generated."
                );
                pregenActiveSet.remove(worldName);
                return;
            }

            TaskBuilder.Countdown genTaskBuilder = TaskBuilder.countdown();

            genTaskBuilder.setAction((times) -> {
                long startTime = System.nanoTime();
                long maxTime = TimeUtil.convertTicksToNanos(1);

                while (!toGenChunkList.isEmpty() && System.nanoTime() - startTime < maxTime) {
                    BlockVector2 chunkCoords = toGenChunkList.remove(toGenChunkList.size() - 1);
                    world.loadChunk(chunkCoords.getX(), chunkCoords.getZ());
                }

                if (ChanceUtil.getChance(20 * 5)) {
                    ChatUtil.sendAdminNotice(
                        ChatColor.YELLOW,
                        noticePrefix,
                        Text.of(ChatColor.WHITE, ChatUtil.WHOLE_NUMBER_FORMATTER.format(toGenChunkList.size())),
                        " chunks left to generate."
                    );
                }

                return toGenChunkList.isEmpty();
            });

            genTaskBuilder.setFinishAction(() -> {
                ChatUtil.sendAdminNotice(
                    ChatColor.YELLOW,
                    noticePrefix,
                    "Generation complete."
                );
                pregenActiveSet.remove(worldName);
            });

            genTaskBuilder.build();
        });

        walkerTaskBuilder.build();
    }
}