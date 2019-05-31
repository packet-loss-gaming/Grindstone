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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Protected Dropped Items", desc = "Protect specific dropped items.")
@Depend(components = {SessionComponent.class})
public class ProtectedDroppedItemsComponent extends BukkitComponent implements Listener, Runnable {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private static final UUID UNKNOWN_UUID = UUID.randomUUID();

    @InjectComponent
    private SessionComponent sessions;

    private Map<Item, OfflinePlayer> itemProtectionMapping = new HashMap<>();

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


    public void protectDrop(Item item, OfflinePlayer offlinePlayer) {
        itemProtectionMapping.put(item, offlinePlayer);
    }

    public void protectDrop(Item item, UUID playerID) {
        protectDrop(item, Bukkit.getOfflinePlayer(playerID));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item)) {
            return;
        }

        Item entity = (Item) event.getEntity();
        if (itemProtectionMapping.containsKey(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemPickup(PlayerPickupItemEvent event) {
        OfflinePlayer owningPlayer = itemProtectionMapping.get(event.getItem());
        if (owningPlayer == null) {
            return;
        }

        Player interactingPlayer = event.getPlayer();

        UUID playerID = interactingPlayer.getUniqueId();
        UUID ownerID = owningPlayer.getUniqueId();

        if (!ownerID.equals(playerID)) {
            DropSession session = sessions.getSession(DropSession.class, interactingPlayer);
            if (session.shouldNotify()) {
                ChatUtil.sendError(interactingPlayer, "That item can only be picked up by " + owningPlayer.getName());
                session.notified();
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemMergeEvent(ItemMergeEvent event) {
        Item source = event.getEntity();
        Item destination = event.getTarget();

        OfflinePlayer sourcePlayer = itemProtectionMapping.get(source);
        OfflinePlayer destinationPlayer = itemProtectionMapping.get(destination);

        UUID sourceOwner = sourcePlayer == null ? UNKNOWN_UUID : sourcePlayer.getUniqueId();
        UUID destinationOwner = destinationPlayer == null ? UNKNOWN_UUID : destinationPlayer.getUniqueId();

        if (!sourceOwner.equals(destinationOwner)) {
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
