/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.invite.db.InviteResult;
import gg.packetloss.grindstone.invite.db.PlayerInviteDatabase;
import gg.packetloss.grindstone.invite.db.mysql.MySQLPlayerInviteDatabase;
import gg.packetloss.grindstone.util.task.promise.FailableTaskFuture;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import gg.packetloss.grindstone.warps.WarpQualifiedName;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@ComponentInformation(friendlyName = "Player Invites", desc = "Player invite system.")
@Depend(components = {ChatBridgeComponent.class, ManagedWorldComponent.class, WarpsComponent.class})
public class PlayerInviteComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private WarpsComponent warps;

    private PlayerInviteDatabase inviteDatabase = new MySQLPlayerInviteDatabase();

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.register(InviteCommandsRegistration.builder(), new InviteCommands(this));
        });
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("uninvited-players.allow")
        public boolean allowUninvitedPlayers = true;
        @Setting("uninvited-players.rejection-message")
        public String rejectionMessage = "Sorry, uninvited new players are not allowed at this time.";
    }

    private void notifyAdminOfInvite(UUID currentPlayer, UUID newPlayer) {
        TaskFuture.asyncTask(() -> {
            PlayerProfile currentPlayerProfile = Bukkit.getServer().createProfile(currentPlayer);
            PlayerProfile newPlayerProfile = Bukkit.getServer().createProfile(newPlayer);

            currentPlayerProfile.complete(false, true);
            newPlayerProfile.complete(false, true);

            chatBridge.modBroadcast(
                    currentPlayerProfile.getName() + " invited " + newPlayerProfile.getName() + "."
            );
            return null;
        });
    }

    public FailableTaskFuture<InviteResult, Void> invitePlayer(UUID currentPlayer, UUID newPlayer) {
        notifyAdminOfInvite(currentPlayer, newPlayer);
        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(inviteDatabase.addInvite(currentPlayer, newPlayer));
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }

    public FailableTaskFuture<Optional<UUID>, Void> getInvitingPlayer(UUID newPlayer) {
        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(inviteDatabase.getInvitor(newPlayer));
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }

    private WarpQualifiedName getInviteDestinationWarpFor(UUID invitor) {
        return new WarpQualifiedName(invitor, "invitee");
    }

    public boolean setInviteDestination(UUID invitor, Location location) {
        return warps.setWarp(getInviteDestinationWarpFor(invitor), location);
    }

    private List<Supplier<Optional<Location>>> getInviteDestinations(UUID invitor) {
        return List.of(
            () -> warps.getWarp(getInviteDestinationWarpFor(invitor)),
            () -> warps.getWarp(warps.getHomeWarpQualifierFor(invitor))
        );
    }

    public Optional<Location> getInviteDestination(UUID invitor) {
        // Try to resolve the best destination.
        for (Supplier<Optional<Location>> destinationSupplier : getInviteDestinations(invitor)) {
            Optional<Location> optDestination = destinationSupplier.get();
            if (optDestination.isEmpty()) {
                continue;
            }

            Location destination = optDestination.get();
            // The sky world cannot be an invite destination.
            // FIXME: This should share code with the portal component's resolvers, instead of hard
            // coding the sky world.
            if (managedWorld.is(ManagedWorldIsQuery.SKY, destination.getWorld())) {
                continue;
            }

            return optDestination;
        }

        return Optional.empty();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        if (config.allowUninvitedPlayers) {
            return;
        }

        if (inviteDatabase.getInvitor(event.getUniqueId()).isPresent()) {
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Component.text(config.rejectionMessage));
    }
}
