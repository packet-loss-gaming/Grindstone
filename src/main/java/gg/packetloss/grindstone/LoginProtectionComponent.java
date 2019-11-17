/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Login Protection", desc = "Get stuff the first time you come.")
@Depend(plugins = {"WorldGuard"})
public class LoginProtectionComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private Set<Player> protectedPlayers = new HashSet<>();

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private void enableProtection(Player player) {
        protectedPlayers.add(player);
        server.getOnlinePlayers().forEach(p -> p.hidePlayer(inst, player));
    }

    private void disableProtection(Player player) {
        protectedPlayers.remove(player);
        server.getOnlinePlayers().forEach(p -> p.showPlayer(inst, player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        enableProtection(player);

        ChatUtil.sendNotice(player, ChatColor.GOLD + "Resource pack load protection enabled.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePackEvent(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();

        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            return;
        }

        disableProtection(player);

        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED:
                ChatUtil.sendNotice(player, ChatColor.GOLD + "Resource pack load protection disabled (loaded).");
                break;
            case FAILED_DOWNLOAD:
                ChatUtil.sendError(player, ChatColor.RED + "Resource pack load protection disabled (failed to load).");
                break;
            case DECLINED:
                ChatUtil.sendNotice(player, ChatColor.GOLD + "Resource pack load protection disabled (declined).");
                break;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        disableProtection(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTargetEvent(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof Player)) {
            return;
        }

        Player player = (Player) target;
        if (protectedPlayers.contains(target)) {
            event.setCancelled(true);
        }
    }
}