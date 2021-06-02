/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.sky;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.invite.InviteList;
import gg.packetloss.grindstone.util.listener.DoorRestorationListener;
import gg.packetloss.grindstone.util.listener.NaturalSpawnBlockingListener;
import gg.packetloss.grindstone.util.listener.NuisanceSpawnBlockingListener;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Sky Core", desc = "Operate the core sky world functionality.")
@Depend(components = {ManagedWorldComponent.class, PlayerHistoryComponent.class})
public class SkyWorldCoreComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PlayerHistoryComponent playerHistory;

    private final InviteList inviteList = new InviteList();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        CommandBook.registerEvents(new DoorRestorationListener(this::isSkyWorld));
        CommandBook.registerEvents(new NaturalSpawnBlockingListener(this::isSkyWorld));
        CommandBook.registerEvents(new NuisanceSpawnBlockingListener(this::isSkyWorld));

        // Register commands
        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            registrar.registerAsSubCommand("sky", "Sky world commands", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, SkyCommandsRegistration.builder(), new SkyCommands(this));
            });
        });
    }

    public boolean isSkyWorld(World world) {
        return managedWorld.is(ManagedWorldIsQuery.SKY, world);
    }

    public void registerInvite(Player issuer, Player invitee) {
        Validate.isTrue(!isForbiddenFromEntering(invitee));

        inviteList.invite(issuer.getUniqueId(), invitee.getUniqueId());
    }

    public CompletableFuture<Boolean> acceptInvite(Player invitee) {
        UUID inviteeID = invitee.getUniqueId();
        if (inviteList.accept(inviteeID)) {
            UUID senderID = inviteList.getIssuer(inviteeID);

            Player player = Bukkit.getPlayer(senderID);
            assert player != null;

            ChatUtil.sendNotice(player, invitee.getDisplayName() + " accepted your invite.");
            return invitee.teleportAsync(player.getLocation());
        }

        return CompletableFuture.completedFuture(false);
    }

    public boolean isForbiddenFromEntering(Player player) {
        return player.hasPermission("aurora.severe-offense");
    }

    public boolean hasAccess(Player player) {
        if (isForbiddenFromEntering(player)) {
            return false;
        }

        if (inviteList.containsAccepted(player.getUniqueId())) {
            return true;
        }

        if (player.hasPermission("aurora.skyworld.override")) {
            return true;
        }

        try {
            return playerHistory.getTimePlayed(player).get() >= TimeUnit.DAYS.toSeconds(30);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!isSkyWorld(event.getTo().getWorld())) {
            return;
        }

        if (hasAccess(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        if (!isSkyWorld(event.getEntity().getWorld())) {
            return;
        }

        event.setCancelled(true);

        Location from = event.getEntity().getLocation();

        World city = managedWorld.get(ManagedWorldGetQuery.CITY);
        Location spawn = city.getSpawnLocation();
        double entryRadius = 500;

        event.getEntity().teleport(new Location(
                city,
                spawn.getX() + ((Math.abs(from.getBlockX()) % entryRadius) - (entryRadius / 2)),
                260,
                spawn.getZ() + ((Math.abs(from.getBlockZ()) % entryRadius) - (entryRadius / 2))
        ), PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void expelPlayer(Player player) {
        if (!isSkyWorld(player.getWorld())) {
            return;
        }

        player.teleportAsync(managedWorld.get(ManagedWorldGetQuery.CITY).getSpawnLocation()).thenAccept(teleported -> {
            if (teleported) {
                ChatUtil.sendNotice(player, "Your host has left, thanks for visiting!");
            } else {
                // Shut everything down, we couldn't remove this player, this should not ever be allowed.
                Thread.dumpStack();
                Bukkit.shutdown();
            }
        });
    }

    public void revokeAllInvitedBy(UUID inviter) {
        for (UUID revokedID : inviteList.revokeAllInvitesBy(inviter)) {
            Player revokedPlayer = Bukkit.getPlayer(revokedID);
            if (revokedPlayer == null) {
                continue;
            }

            expelPlayer(revokedPlayer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeaveByTeleport(PlayerChangedWorldEvent event) {
        revokeAllInvitedBy(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeaveByQuit(PlayerQuitEvent event) {
        revokeAllInvitedBy(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isSkyWorld(player.getWorld())) {
            return;
        }

        if (hasAccess(player)) {
            return;
        }

        expelPlayer(player);
    }
}
