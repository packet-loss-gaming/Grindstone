/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.region;

import com.sk89q.worldedit.regions.Region;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.math.BigDecimal;
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

    private void walkBlock(ReportSourceInfo reportSource, World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        computeBlockName(block).ifPresent((blockName) -> {
            reportSource.allNames.add(blockName);
            reportSource.blockCounts.merge(blockName, 1, Integer::sum);
        });

        if (includeItems) {
            BlockState blockState = block.getState();
            if (blockState instanceof Chest) {
                walkInventory(reportSource, ((Chest) blockState).getBlockInventory());
            } else if (blockState instanceof Container) {
                walkInventory(reportSource, ((Container) blockState).getInventory());
            }
        }
    }

    public CompletableFuture<RegionValueReport> walkRegion(Region region, World world) {
        ReportSourceInfo reportSource = new ReportSourceInfo();

        RegionWalker.walk(region, (x, y, z) -> {
            walkBlock(reportSource, world, x, y, z);
        });

        CompletableFuture<RegionValueReport> future = new CompletableFuture<>();

        PluginTaskExecutor.submitAsync(() -> {
            MarketItemLookupInstance nameItemMapping = MarketComponent.getLookupInstance(reportSource.allNames);

            BigDecimal blockPrice = BigDecimal.ZERO;
            for (Map.Entry<String, Integer> entry : reportSource.blockCounts.entrySet()) {
                BigDecimal singleBlockPrice = nameItemMapping.checkSellPrice(entry.getKey()).orElse(BigDecimal.ZERO);
                BigDecimal blockCount = BigDecimal.valueOf(entry.getValue());

                blockPrice = blockPrice.add(singleBlockPrice.multiply(blockCount));
            }

            BigDecimal itemPrice = BigDecimal.ZERO;
            BigDecimal maximumItemValue = BigDecimal.ZERO;
            for (ItemStack entry : reportSource.items) {
                BigDecimal currItemPrice = nameItemMapping.checkSellPrice(entry).orElse(BigDecimal.ZERO);
                itemPrice = itemPrice.add(currItemPrice);

                BigDecimal possItemPrice = nameItemMapping.checkMaximumValue(entry).orElse(BigDecimal.ZERO);
                maximumItemValue = maximumItemValue.add(possItemPrice);
            }

            future.complete(new RegionValueReport(blockPrice, itemPrice, maximumItemValue));
        });

        return future;
    }

    private static class ReportSourceInfo {
        public Set<String> allNames = new HashSet<>();
        public Map<String, Integer> blockCounts = new HashMap<>();
        public List<ItemStack> items = new ArrayList<>();
    }
}
