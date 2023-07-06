/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.PlayerDropMapping;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.List;
import java.util.UUID;

@ComponentInformation(friendlyName = "Garbage Disposal", desc = "Get rid of that unwanted trash.")
public class GarbageDisposalComponent extends BukkitComponent implements Listener {
    private PlayerDropMapping dropMapping = new PlayerDropMapping();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), dropMapping, 0, 20 * 15);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        dropMapping.trackItem(event.getPlayer(), event.getItemDrop());
    }

    public void clearDropsOfPlayer(UUID playerID) {
        // Remove the items
        List<Item> drops = dropMapping.getDropsList(playerID);
        for (Item item : drops) {
            if (!item.isValid()) {
                continue;
            }

            SingleBlockParticleEffect.puffOfSmoke(item.getLocation());
            item.remove();
        }

        // Cleanup the list so the watcher doesn't have to
        drops.clear();
    }

    public void clearDropsOfPlayer(Player player) {
        clearDropsOfPlayer(player.getUniqueId());
    }

    public class Commands {
        @Command(aliases = {"clearmydrops"}, desc = "Clear player's drops", max = 0)
        public void clearMyDrops(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            clearDropsOfPlayer(player);

            ChatUtil.sendNotice(player, "Drops cleared!");
        }
    }
}
