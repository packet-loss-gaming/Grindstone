package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockType;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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
            EffectUtil.Strange.mobBarrage(player, pLoc, EntityType.BAT, true);
        }
    }

    @Override
    public void run() {

        Player[] players = server.getOnlinePlayers();
        butterBoot(players);
    }

    private void butterBoot(final Player[] players) {

        for (Player player : players) {

            if (!player.isValid()) continue;

            ItemStack boots = player.getInventory().getBoots();
            if (!ItemUtil.matchesFilter(boots, ChatColor.GOLD + "Butter Boots")) continue;

            Location loc = player.getLocation();
            Vector additive = player.getLocation().getDirection().multiply(-1);
            loc.add(additive);

            final Location locClone = loc.clone();

            final int type = loc.getBlock().getTypeId();
            final byte data = loc.getBlock().getData();

            if (!BlockType.canPassThrough(type)) continue;
            for (Player aPlayer : players) {

                aPlayer.sendBlockChange(loc, Material.FIRE, (byte) 0);
            }

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    for (Player aPlayer : players) {

                        aPlayer.sendBlockChange(locClone, type, data);
                    }
                }
            }, 20 * 8);
        }
    }
}
