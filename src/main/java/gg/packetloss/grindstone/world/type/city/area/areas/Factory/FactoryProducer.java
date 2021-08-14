/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

public class FactoryProducer {
    private final FactoryArea parent;
    private final Map<FactoryOutputKind, ProtectedRegion[]> outputMap;

    public FactoryProducer(FactoryArea parent, Map<FactoryOutputKind, ProtectedRegion[]> outputMap) {
        this.parent = parent;
        this.outputMap = outputMap;
    }

    private ProtectedRegion getChamber(FactoryOutputKind type) {
        return CollectionUtil.getElement(outputMap.get(type));
    }

    private FactoryOutputKind getProductType(ItemStack product) {
        Material productType = product.getType();
        if (ItemUtil.isIngot(productType)) {
            return FactoryOutputKind.SMELTING_CHAMBER;
        }
        return FactoryOutputKind.FOUNTAIN;
    }

    private Location getSpawnPoint(ProtectedRegion protectedRegion) {
        Region region = RegionUtil.convert(protectedRegion).orElseThrow();
        Location spawnLoc = RegionUtil.getCenter(parent.getWorld(), protectedRegion);

        // Divide by 2 since we're center, then subtract 1/2 a block divided by 2 (1/4 block)
        // so that the items don't get stuck in the sides of the device.
        double xWidth = (RegionUtil.getXWidth(region).orElseThrow() / 2.0) - .25;
        double zWidth = (RegionUtil.getZWidth(region).orElseThrow() / 2.0) - .25;

        if (xWidth > zWidth) {
            spawnLoc.add(ChanceUtil.getRangedRandom(-xWidth, xWidth), 0, 0);
        } else {
            spawnLoc.add(0, 0, ChanceUtil.getRangedRandom(-zWidth, zWidth));
        }

        return spawnLoc;
    }

    public Location getSpawnPoint(FactoryOutputKind outputKind) {
        return getSpawnPoint(getChamber(outputKind));
    }

    private boolean checkCanProduceAtOutput(FactoryOutputKind outputKind) {
        // FIXME: This is a bit dirty.
        if (outputKind != FactoryOutputKind.SMELTING_CHAMBER) {
            return true;
        }

        if (parent.lavaSupply.getLavaCount() == 0) {
            return false;
        }

        if (ChanceUtil.getChance(parent.getConfig().productionLavaDrainChance)) {
            parent.lavaSupply.removeLava(1);
        }

        return true;
    }

    public void produce(FactoryJob job) {
        ItemStack product = job.produceItem();
        FactoryOutputKind outputKind = getProductType(product);
        if (!checkCanProduceAtOutput(outputKind)) {
            job.increaseProduction(1);
            return;
        }

        Item item = EntityUtil.spawnProtectedItem(product, job.getPlayerID(), getSpawnPoint(outputKind));
        item.setVelocity(new Vector());
    }
}
