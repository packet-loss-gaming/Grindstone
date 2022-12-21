/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.invite.db.InviteResult;
import gg.packetloss.grindstone.invite.db.InviteStatus;
import gg.packetloss.grindstone.invite.db.PlayerInviteDatabase;
import gg.packetloss.grindstone.invite.db.mysql.MySQLPlayerInviteDatabase;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@ComponentInformation(friendlyName = "Player Invites", desc = "Player invite system.")
@Depend(components = {
    AdminComponent.class,
    ChatBridgeComponent.class,
    ManagedWorldComponent.class,
    PlayerStateComponent.class,
    WarpsComponent.class
})
public class PlayerInviteComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PlayerStateComponent playerState;
    @InjectComponent
    private WarpsComponent warps;

    private final PlayerInviteDatabase inviteDatabase = new MySQLPlayerInviteDatabase();

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

    private ItemStack pickRandomReward() {
        if (!ChanceUtil.getChance(5)) {
            return CustomItemCenter.build(
                CustomItems.PHANTOM_DIAMOND,
                ChanceUtil.getRandomNTimes(64, 5)
            );
        }
        return ChanceUtil.supplyRandom(
            () -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_HELMET),
            () -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_CHESTPLATE),
            () -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_LEGGINGS),
            () -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_BOOTS),
            () -> CustomItemCenter.build(CustomItems.PHANTOM_CLOCK),
            () -> CustomItemCenter.build(CustomItems.LINEAR_AXE),
            () -> CustomItemCenter.build(CustomItems.LINEAR_PICKAXE),
            () -> CustomItemCenter.build(CustomItems.LINEAR_SHOVEL),
            () -> CustomItemCenter.build(CustomItems.LINEAR_BLOCK_PLACER),
            () -> CustomItemCenter.build(CustomItems.RADIAL_AXE),
            () -> CustomItemCenter.build(CustomItems.RADIAL_PICKAXE),
            () -> CustomItemCenter.build(CustomItems.RADIAL_SHOVEL),
            () -> CustomItemCenter.build(CustomItems.MAD_MILK),
            () -> CustomItemCenter.build(CustomItems.EXECUTIONER_AXE),
            () -> CustomItemCenter.build(CustomItems.ROGUE_OATH, ChanceUtil.getRandom(5)),
            () -> CustomItemCenter.build(CustomItems.NINJA_OATH, ChanceUtil.getRandom(5)),
            () -> {
                if (ChanceUtil.getChance(15)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_CROWN);
                }
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_HELMET);
                }
                return CustomItemCenter.build(CustomItems.ANCIENT_HELMET);
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_CHESTPLATE);
                }
                return CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE);
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_LEGGINGS);
                }
                return CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS);
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_BOOTS);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_BOOTS);
                }
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.NECROS_HELMET);
                }
                return CustomItemCenter.build(CustomItems.NECTRIC_HELMET);
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.NECROS_CHESTPLATE);
                }
                return CustomItemCenter.build(CustomItems.NECTRIC_CHESTPLATE);
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.NECROS_LEGGINGS);
                }
                return CustomItemCenter.build(CustomItems.NECTRIC_LEGGINGS);
            },
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.NECROS_BOOTS);
                } else {
                    return CustomItemCenter.build(CustomItems.NECTRIC_BOOTS);
                }
            },
            () -> {
                if (ChanceUtil.getChance(15)) {
                    if (ChanceUtil.getChance(2)) {
                        return CustomItemCenter.build(CustomItems.HYMN_OF_SUMMATION);
                    } else {
                        return CustomItemCenter.build(CustomItems.HYMN_OF_HARVEST);
                    }
                }
                return CustomItemCenter.build(CustomItems.CHICKEN_HYMN);
            },
            () -> CustomItemCenter.build(CustomItems.CHICKEN_BOW),
            () -> CustomItemCenter.build(
                CustomItems.SCROLL_OF_SUMMATION,
                ChanceUtil.getRandomNTimes(64, 3)
            ),
            () -> {
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.CALMING_CRYSTAL);
                }
                return CustomItemCenter.build(
                    CustomItems.PATIENT_X_THERAPY_NOTES,
                    ChanceUtil.getRandomNTimes(64, 5)
                );
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_LIFE);
                }
                if (ChanceUtil.getChance(30)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_LEGENDS);
                }
                if (ChanceUtil.getChance(30)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_DIVINITY);
                }
                if (ChanceUtil.getChance(8)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_POISON);
                }
                if (ChanceUtil.getChance(8)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_SACRIFICE);
                }
                if (ChanceUtil.getChance(5)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_CURSED_SMELTING);
                }
                if (ChanceUtil.getChance(3)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_THE_UNDEAD);
                }
                if (ChanceUtil.getChance(2)) {
                    return CustomItemCenter.build(CustomItems.TOME_OF_THE_CLEANLY);
                }
                return CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER);
            }
        );
    }

    public TaskFuture<Void> redeemPlayerReferrals(UUID currentPlayer) {
        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(inviteDatabase.getInvitesByStatus(currentPlayer, InviteStatus.JOINED));
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        }).thenApplyAsynchronously(
            (inviteIDs) -> {
                inviteDatabase.setInviteStatus(inviteIDs, InviteStatus.REDEEMED);
                return inviteIDs;
            },
            (ignored) -> {}
        ).thenApplyFailable(
            (inviteIDs) -> {
                List<Integer> untocuhedInviteIDs = Lists.newArrayList(inviteIDs);

                try {
                    if (inviteIDs.size() > 0) {
                        Player player = Bukkit.getPlayer(currentPlayer);

                        if (player != null && !playerState.hasTempKind(player) && !admin.isAdmin(player)) {
                            for (Integer inviteID : inviteIDs) {
                                GeneralPlayerUtil.giveItemToPlayer(player, pickRandomReward());
                                ChatUtil.sendNotice(player, "Thank you for your referral!");

                                // The reward has been handed out, even if we error, do not reset this reward.
                                untocuhedInviteIDs.remove(inviteID);
                            }
                            return TaskResult.of(null);
                        }
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
                return TaskResult.failed(untocuhedInviteIDs);
            }
        ).thenAcceptAsynchronously(
            (ignored) -> { },
            (inviteIDs) -> { inviteDatabase.setInviteStatus(inviteIDs, InviteStatus.JOINED); }
        );
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
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        if (config.allowUninvitedPlayers) {
            return;
        }

        if (inviteDatabase.getInvitor(event.getUniqueId()).isPresent()) {
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Component.text(config.rejectionMessage));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();

        // Just blindly update in an async task, this checks to ensure they have the right status.
        TaskFuture.asyncTask(() -> {
            inviteDatabase.markNewPlayerJoined(playerID);
            return null;
        });


        // Grab any pending invites this player has.
        redeemPlayerReferrals(playerID);
    }
}