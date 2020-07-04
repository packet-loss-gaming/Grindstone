package gg.packetloss.grindstone.prettyfier;

import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class AutoCloser implements Prettyfier, Listener {
    private Map<Location, DebounceHandle<Location>> pendingCloses = new HashMap<>();

    @Override
    public void forceFinish() {
        pendingCloses.values().forEach(DebounceHandle::bounceNow);
    }

    private Block getClosableBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return null;
        }

        Block clicked = event.getClickedBlock();
        if (!EnvironmentUtil.isClosable(clicked)) {
            return null;
        }
        Block lowerBlock = clicked.getRelative(BlockFace.DOWN);
        if (EnvironmentUtil.isClosable(lowerBlock)) {
            clicked = lowerBlock;
        }

        return clicked;
    }

    private void createNewHandle(Location blockLoc) {
        TaskBuilder.Debounce<Location> builder = TaskBuilder.debounce();
        builder.setWaitTime(20 * 10);

        builder.setInitialValue(blockLoc);
        builder.setUpdateFunction((oldBlock, newBlock) -> newBlock);

        builder.setBounceAction((location) -> {
            Block block = location.getBlock();
            if (EnvironmentUtil.isClosable(block)) {
                Openable openable = (Openable) block.getBlockData();
                openable.setOpen(false);
                block.setBlockData(openable);
            }

            pendingCloses.remove(location);
        });

        DebounceHandle<Location> newHandle = builder.build();
        newHandle.accept(blockLoc);
        pendingCloses.put(blockLoc, newHandle);
    }

    @EventHandler
    public void onDoorInteract(PlayerInteractEvent event) {
        Block closable = getClosableBlock(event);
        if (closable == null) {
            return;
        }

        DebounceHandle<Location> existingHandle = pendingCloses.get(closable.getLocation());
        if (existingHandle != null) {
            existingHandle.accept(closable.getLocation());
            return;
        }

        createNewHandle(closable.getLocation());
    }
}
