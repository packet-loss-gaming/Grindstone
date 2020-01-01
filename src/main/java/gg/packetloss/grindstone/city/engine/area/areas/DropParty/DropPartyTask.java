/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.DropParty;

import com.sk89q.worldedit.regions.CuboidRegion;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Iterator;
import java.util.List;

public class DropPartyTask {

    private TimedRunnable runnable;
    private World world;
    private CuboidRegion rg;
    private List<ItemStack> items;
    private RegionChecker checker;
    private int xpAmt = 0;
    private int xpSize = 0;

    public DropPartyTask(World world, CuboidRegion rg, List<ItemStack> items, RegionChecker checker) {
        this.world = world;
        this.rg = rg;
        this.items = items;
        this.checker = checker;
        this.runnable = new TimedRunnable(create(), (int) (items.size() * .15) + 1);
    }

    public void start(Plugin plugin, BukkitScheduler scheduler) {
        start(plugin, scheduler, 0, 20);
    }

    public void start(Plugin plugin, BukkitScheduler scheduler, long delay, long interval) {
        runnable.setTask(scheduler.runTaskTimer(plugin, runnable, delay, interval));
    }

    public World getWorld() {
        return world;
    }

    public void setXPChance(int amt) {
        xpAmt = amt;
    }

    public void setXPSize(int size) {
        xpSize = size;
    }

    private IntegratedRunnable create() {
        return new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                Iterator<ItemStack> it = items.iterator();

                for (int k = 10; it.hasNext() && k > 0; k--) {

                    // Pick a random Location
                    Location l = LocationUtil.pickLocation(world, rg.getMaximumY(), checker);
                    if (!LocationUtil.isChunkLoadedAt(l)) {
                        break;
                    }

                    world.dropItem(l, it.next());

                    // Remove the drop
                    it.remove();

                    // Drop the xp
                    if (xpAmt > 0) {
                        // Throw in some xp cause why not
                        for (int s = ChanceUtil.getRandom(xpAmt); s > 0; --s) {
                            ExperienceOrb e = world.spawn(l, ExperienceOrb.class);
                            e.setExperience(xpSize);
                        }
                    }
                }

                // Cancel if we've ran out of drop party pulses or if there is nothing more to drop
                if (items.isEmpty()) {
                    runnable.cancel();
                }
                return true;
            }

            @Override
            public void end() {

            }
        };
    }
}
