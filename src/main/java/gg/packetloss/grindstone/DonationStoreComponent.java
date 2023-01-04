/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Donation Store", desc = "Effect manager for donations.")
public class DonationStoreComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        //server.getScheduler().runTaskTimer(inst, this, 20, 1);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        Location pLoc = player.getLocation();

        if (inst.hasPermission(player, "aurora.deatheffects.bat")) {
            EffectUtil.Strange.mobBarrage(pLoc, Bat.class);
        }
    }

    @Override
    public void run() {

        Collection<? extends Player> players = server.getOnlinePlayers();
        butterBoot(players);
    }

    private void butterBoot(final Collection<? extends Player> players) {

        for (Player player : players) {

            if (!player.isValid()) continue;

            ItemStack boots = player.getInventory().getBoots();
            if (!ItemUtil.matchesFilter(boots, ChatColor.GOLD + "Butter Boots")) continue;

            Location loc = player.getLocation();
            Vector additive = player.getLocation().getDirection().multiply(-1);
            loc.add(additive);

            Location locClone = loc.clone();

            Block block = loc.getBlock();
            BlockData blockData = block.getBlockData();

            if (EnvironmentUtil.isAirBlock(block)) continue;

            for (Player aPlayer : players) {
                aPlayer.sendBlockChange(loc, Material.FIRE, (byte) 0);
            }

            server.getScheduler().runTaskLater(inst, () -> {
                for (Player aPlayer : players) {
                    aPlayer.sendBlockChange(locClone, blockData);
                }
            }, 20 * 8);
        }
    }
}
