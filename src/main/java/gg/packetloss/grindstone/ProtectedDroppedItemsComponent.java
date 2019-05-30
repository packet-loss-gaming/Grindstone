/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Protected Dropped Items", desc = "Protect specific dropped items.")
@Depend(components = {SessionComponent.class})
public class ProtectedDroppedItemsComponent extends BukkitComponent implements Listener, Runnable {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;

    private Map<Item, Function<Player, Boolean>> itemProtectionMapping = new HashMap<>();

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 20 * 15);
    }

    @Override
    public void run() {
        itemProtectionMapping.entrySet().removeIf(entry -> !entry.getKey().isValid());
    }

    public void protectDrop(Item item, Function<Player, Boolean> callback) {
        itemProtectionMapping.put(item, callback);
    }

    public void protectDrop(Item item, OfflinePlayer player) {
        protectDrop(item, (interactingPlayer) -> {
            boolean idsMatch = interactingPlayer.getUniqueId().equals(player.getUniqueId());
            if (idsMatch) {
                return false;
            }

            DropSession session = sessions.getSession(DropSession.class, interactingPlayer);
            if (session.shouldNotify()) {
                ChatUtil.sendError(interactingPlayer, "That item can only be picked up by " + player.getName());
                session.notified();
            }

            return true;
        });
    }

    public void protectDrop(Item item, UUID playerID) {
        protectDrop(item, server.getOfflinePlayer(playerID));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemPickup(PlayerPickupItemEvent event) {
        Function<Player, Boolean> callback = itemProtectionMapping.get(event.getItem());
        if (callback != null && callback.apply(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private static class DropSession extends PersistentSession {
        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private long lastNotice = 0;

        protected DropSession() {
            super(MAX_AGE);
        }

        public boolean shouldNotify() {
            return lastNotice == 0 || System.currentTimeMillis() - lastNotice > TimeUnit.SECONDS.toMillis(5);
        }

        public void notified() {
            this.lastNotice = System.currentTimeMillis();
        }
    }
}
