/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldIsQuery;
import gg.packetloss.grindstone.managedworld.ManagedWorldMassQuery;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.listener.DoorRestorationListener;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Range Core", desc = "Operate the range worlds.")
@Depend(components = {ManagedWorldComponent.class})
public class RangeCoreComponent extends BukkitComponent implements Listener, Runnable {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private ManagedWorldComponent managedWorld;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        CommandBook.registerEvents(new DoorRestorationListener(this::isRangeWorld));

        // Start tree growth task
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 20 * 2);
    }

    private void autoReplantSaplings(World world) {
        if (world.getTime() % 7 != 0) {
            return;
        }

        for (Entity item : world.getEntitiesByClasses(Item.class)) {
            ItemStack stack = ((Item) item).getItemStack();

            if (item.getTicksLived() > 20 * 60 && EnvironmentUtil.isSapling(stack.getType())) {
                Block block = item.getLocation().getBlock();
                if (EnvironmentUtil.canTreeGrownOn(block.getRelative(BlockFace.DOWN))) {
                    if (stack.getAmount() > 1) {
                        ItemStack newStack = stack.clone();
                        newStack.setAmount(stack.getAmount() - 1);
                        item.getWorld().dropItem(item.getLocation(), newStack);
                    }

                    block.setType(stack.getType(), true);
                    item.remove();
                }
            }
        }
    }

    @Override
    public void run() {
        List<World> rangeWorlds = managedWorld.getAll(ManagedWorldMassQuery.RANGE_OVERWORLDS);
        for (World world : rangeWorlds) {
            autoReplantSaplings(world);
        }
    }

    private boolean isRangeWorld(World world) {
        return managedWorld.is(ManagedWorldIsQuery.ANY_RANGE, world);
    }

    private boolean shouldBlockExplosionAt(Block block) {
        if (!isRangeWorld(block.getWorld())) {
            return false;
        }

        return block.getY() > block.getWorld().getSeaLevel() - 5;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (shouldBlockExplosionAt(event.getLocation().getBlock())) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (shouldBlockExplosionAt(event.getBlock())) {
            event.blockList().clear();
        }
    }
}