/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.items.flight.FlightItemsComponent;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.listener.BetterMobSpawningListener;
import gg.packetloss.grindstone.util.listener.DoorRestorationListener;
import gg.packetloss.grindstone.util.listener.NuisanceSpawnBlockingListener;
import gg.packetloss.grindstone.util.listener.combatwatchdog.UnbalancedCombatWatchdog;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldMassQuery;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
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

@ComponentInformation(friendlyName = "Range Core", desc = "Operate the range worlds.")
@Depend(components = {ManagedWorldComponent.class, FlightItemsComponent.class})
public class RangeCoreComponent extends BukkitComponent implements Listener, Runnable {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private FlightItemsComponent flightItems;

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        // Process the configuration 1 tick late to make sure all worlds are loaded at the time of processing.
        Bukkit.getScheduler().runTask(CommandBook.inst(), this::processConfig);

        CommandBook.registerEvents(this);
        CommandBook.registerEvents(new UnbalancedCombatWatchdog(this::isRangeWorld));
        CommandBook.registerEvents(new DoorRestorationListener(this::isRangeWorld));
        CommandBook.registerEvents(new NuisanceSpawnBlockingListener(this::isRangeWorld));
        CommandBook.registerEvents(new BetterMobSpawningListener(this::isRangeWorld));

        // Start tree growth task
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 0, 20 * 2);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
        processConfig();
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("world-border.size")
        public double worldBorderSize = 10000;
    }

    public void setWorldBoarder() {
        for (World world : CommandBook.server().getWorlds()) {
            if (!isRangeWorld(world)) {
                continue;
            }

            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setSize(config.worldBorderSize);
            worldBorder.setCenter(world.getSpawnLocation());
        }
    }

    private void processConfig() {
        setWorldBoarder();
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