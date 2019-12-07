package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.support.LinearCreationExecutor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class LinearBlockPlacer extends AbstractItemFeatureImpl {
    private static LinearCreationExecutor executor = new LinearCreationExecutor(CustomItems.LINEAR_BLOCK_PLACER);

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        executor.process(event);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        return ItemUtil.isHoldingItem(event.getPlayer(), CustomItems.LINEAR_BLOCK_PLACER);
    }
}
