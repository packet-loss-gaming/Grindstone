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
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldIsQuery;
import gg.packetloss.grindstone.managedworld.ManagedWorldMassQuery;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
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
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isRangeWorld(player.getWorld())) {
            return;
        }

        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) {
            return;
        }

        MarketItemLookupInstance lookupInstance = MarketComponent.getLookupInstanceFromStacksImmediately(drops);
        drops.sort((o1, o2) -> {
            double o1SellPrice = lookupInstance.checkMaximumValue(o1).orElse(0d);
            double o2SellPrice = lookupInstance.checkMaximumValue(o2).orElse(0d);
            return (int) (o2SellPrice - o1SellPrice);
        });

        Iterator<ItemStack> it = drops.iterator();
        List<ItemStack> grave = new ArrayList<>();
        for (int kept = 9; kept > 0 && it.hasNext(); --kept) {
            ItemStack next = it.next();
            grave.add(next);
            it.remove();

            kept--;
        }

        Location location = player.getLocation();
        Block block = location.getBlock();
        if (WorldGuardBridge.canBuildAt(player, block)) {
            try {
                graveSupplier:
                {
                    checkGrave:
                    {
                        for (BlockFace face : BlockFace.values()) {
                            if (face.getModY() != 0) continue;
                            Block aBlock = block.getRelative(face);
                            if (aBlock.getType() == Material.CHEST) {
                                block = aBlock;
                                break checkGrave;
                            }
                        }
                        block.setType(Material.CHEST, true);
                    }
                    Chest chest = (Chest) block.getState();
                    it = grave.iterator();
                    Inventory blockInv = chest.getInventory();
                    for (int i = 0; it.hasNext(); ++i) {
                        while (blockInv.getItem(i) != null) {
                            ++i;
                            if (i >= blockInv.getSize()) {
                                ChatUtil.sendError(player, "Some items could not be added to your grave!");
                                break graveSupplier;
                            }
                        }
                        blockInv.setItem(i, it.next());
                        it.remove();
                    }
                    ChatUtil.sendNotice(player, "A grave has been created where you died.");
                }
            } catch (Exception ex) {
                log.warning("Location could not be found to create a grave for: " + player.getName());
                ex.printStackTrace();
            }
        }
        event.getDrops().addAll(grave);
    }

    private boolean shouldBlockExplosionAt(Block block) {
        if (!isRangeWorld(block.getWorld())) {
            return false;
        }

        int totalSkyLight = block.getLightFromSky() + block.getRelative(BlockFace.UP).getLightFromSky();
        return totalSkyLight > 0 && block.getY() > 60;
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