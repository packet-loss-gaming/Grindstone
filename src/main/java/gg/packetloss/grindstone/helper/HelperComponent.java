/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.helper;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Helper", desc = "Regex based auto messaging.")
public class HelperComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();

  private Map<String, Response> responses;
  private YAMLResponseList responseList;

  @Override
  public void enable() {
    responseList = new YAMLResponseList(
        new YAMLProcessor(
            new File(inst.getDataFolder().getPath() + "/helper/responses.yml"),
            false,
            YAMLFormat.EXTENDED
        )
    );
    responses = responseList.obtainResponses();

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @Override
  public void reload() {
    responses = responseList.obtainResponses();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAsyncChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    String message = event.getMessage();
    server.getScheduler().runTaskLater(inst, () -> {
      for (Response response : responses.values()) {
        if (response.accept(player, message)) {
          break;
        }
      }
    }, 10);
  }
}
