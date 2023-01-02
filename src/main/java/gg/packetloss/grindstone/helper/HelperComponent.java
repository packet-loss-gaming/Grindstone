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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Helper", desc = "Regex based auto messaging.")
public class HelperComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private List<Response> responses = new ArrayList<>();
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
        configureResponses();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void reload() {
        super.reload();
        configureResponses();
    }

    private void configureResponses() {
        responses.clear();
        responses.add(new CommandResponse());
        responses.addAll(responseList.obtainResponses());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Collection<Player> recipients = event.getRecipients();
        String message = event.getMessage();

        server.getScheduler().runTaskLater(inst, () -> {
            for (Response response : responses) {
                if (response.accept(player, recipients, message)) {
                    break;
                }
            }
        }, 10);
    }
}
