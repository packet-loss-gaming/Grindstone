package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "ADebug", desc = "Debug tools")
public class DebugComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        //inst.registerEvents(new InventoryCorruptionFixer());
        //noinspection AccessStaticViaInstance
        //inst.registerEvents(new BlockDebug());


        // Bug fixes

        // Fixes an issue where potion effects are not removed from players on death
        //noinspection AccessStaticViaInstance
        //inst.registerEvents(new PotionDeathFix());
    }

    private class InventoryCorruptionFixer implements Listener {

        @EventHandler
        public void onLogin(PlayerJoinEvent event) {

            Player player = event.getPlayer();

            if (!player.getName().equals("Dark_Arc")) return;
            ItemStack[] inventory = player.getInventory().getContents();
            inventory = ItemUtil.removeItemOfType(inventory, Material.DOUBLE_PLANT.getId());
            player.getInventory().setContents(inventory);
        }
    }

    private class BlockDebug implements Listener {

        @EventHandler
        public void onRightClick(PlayerInteractEvent event) {

            ItemStack held = event.getItem();
            if (held != null && held.getType() == Material.COAL && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Block block = event.getClickedBlock();
                ChatUtil.sendNotice(event.getPlayer(),
                        "Block name: " + block.getType()
                                + ", Type ID: " + block.getTypeId()
                                + ", Block data: " + block.getData()
                );
                event.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    private class PotionDeathFix implements Listener {

        @EventHandler
        public void onRespawn(final PlayerRespawnEvent event) {

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    Player player = event.getPlayer();

                    for (PotionEffect next : player.getActivePotionEffects()) {
                        player.addPotionEffect(new PotionEffect(next.getType(), 0, 0), true);
                    }
                }
            }, 1);
        }
    }
}
