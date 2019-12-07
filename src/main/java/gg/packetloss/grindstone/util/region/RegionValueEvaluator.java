package gg.packetloss.grindstone.util.region;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeBlockName;
import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemName;

public class RegionValueEvaluator {
    private final boolean includeItems;

    public RegionValueEvaluator(boolean includeItems) {
        this.includeItems = includeItems;
    }

    private void walkInventory(ReportSourceInfo reportSource, Inventory inventory) {
        inventory.forEach((item) -> {
            if (item == null) {
                return;
            }

            // Recurse into any shulker boxes/container items
            if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
                BlockState blockState = ((BlockStateMeta) item.getItemMeta()).getBlockState();
                if (blockState instanceof Container) {
                    walkInventory(reportSource, ((Container) blockState).getInventory());
                }
            }

            computeItemName(item).ifPresent((itemName) -> {
                reportSource.allNames.add(itemName);
                reportSource.items.add(item.clone());
            });
        });
    }

    private void walkBlocK(ReportSourceInfo reportSource, World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        computeBlockName(block.getTypeId(), block.getData()).ifPresent((blockName) -> {
            reportSource.allNames.add(blockName);
            reportSource.blockCounts.merge(blockName, 1, Integer::sum);
        });

        if (includeItems) {
            BlockState blockState = block.getState();
            if (blockState instanceof Container) {
                walkInventory(reportSource, ((Container) blockState).getInventory());
            }
        }
    }

    public CompletableFuture<RegionValueReport> walkRegion(Region region, World world) {
        ReportSourceInfo reportSource = new ReportSourceInfo();

        //noinspection ConstantConditions
        if (region instanceof CuboidRegion) {

            // Doing this for speed
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();

            int minX = min.getBlockX();
            int minY = min.getBlockY();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxY = max.getBlockY();
            int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        walkBlocK(reportSource, world, x, y, z);
                    }
                }
            }

            CompletableFuture<RegionValueReport> future = new CompletableFuture<>();

            CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
                MarketItemLookupInstance nameItemMapping = MarketComponent.getLookupInstance(reportSource.allNames);

                double blockPrice = 0;
                for (Map.Entry<String, Integer> entry : reportSource.blockCounts.entrySet()) {
                    blockPrice += nameItemMapping.checkSellPrice(entry.getKey()).orElse(0d) * entry.getValue();
                }

                double itemPrice = 0;
                double maximumItemValue = 0;
                for (ItemStack entry : reportSource.items) {
                    itemPrice += nameItemMapping.checkSellPrice(entry).orElse(0d);
                    maximumItemValue += nameItemMapping.checkMaximumValue(entry).orElse(0d);
                }

                future.complete(new RegionValueReport(blockPrice, itemPrice, maximumItemValue));
            });

            return future;
        } else {
            throw new UnsupportedOperationException("Unsupported region type");
        }
    }

    private static class ReportSourceInfo {
        public Set<String> allNames = new HashSet<>();
        public Map<String, Integer> blockCounts = new HashMap<>();
        public List<ItemStack> items = new ArrayList<>();
    }
}
