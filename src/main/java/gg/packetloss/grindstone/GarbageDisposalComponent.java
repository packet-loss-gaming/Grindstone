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
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.*;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Garbage Disposal", desc = "Get rid of that unwanted trash.")
public class GarbageDisposalComponent extends BukkitComponent implements Listener, Runnable {
  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  private HashMap<UUID, List<Item>> playerDrops = new HashMap<>();

  @Override
  public void enable() {
    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
    registerCommands(Commands.class);
    server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 20 * 15);
  }

  @Override
  public void run() {
    Set<UUID> onlinePlayerIds = GeneralPlayerUtil.getOnlinePlayerUUIDs();

    Iterator<Map.Entry<UUID, List<Item>>> entrySetIterator = playerDrops.entrySet().iterator();
    while (entrySetIterator.hasNext()) {
      Map.Entry<UUID, List<Item>> entry = entrySetIterator.next();
      UUID playerUUID = entry.getKey();

      if (!onlinePlayerIds.contains(playerUUID)) {
        entrySetIterator.remove();
        continue;
      }

      List<Item> items = entry.getValue();
      items.removeIf(i -> !i.isValid());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    UUID playerID = event.getPlayer().getUniqueId();

    playerDrops.putIfAbsent(playerID, new ArrayList<>());
    List<Item> drops = playerDrops.get(playerID);
    drops.add(event.getItemDrop());
  }

  public void clearDropsOfPlayer(UUID playerID) {
    // Remove the items
    List<Item> drops = playerDrops.getOrDefault(playerID, new ArrayList<>());
    for (Item item : drops) {
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
