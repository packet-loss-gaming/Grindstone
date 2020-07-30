/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ComponentInformation(friendlyName = "Randomized Skulls", desc = "Get a random subset of skulls for players")
public class RandomizedSkullsComponent extends BukkitComponent implements Runnable {
    private final ReadWriteLock randomizedProfilesLock = new ReentrantReadWriteLock();
    private List<PlayerProfile> randomizedProfiles = List.of();
    private AtomicBoolean used = new AtomicBoolean(false);

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        // Create an initial set blocking, then schedule updates every hour
        updatePlayersTo(getRandomSetOfPlayers());
        CommandBook.server().getScheduler().runTaskTimer(
                CommandBook.inst(),
                this,
                TimeUtil.getTicksTillHour(),
                TimeUtil.convertHoursToTicks(1)
        );
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("number-players")
        public int numberOfPlayers = 12;
    }

    @Override
    public void run() {
        // Only update if someone has seen these skulls
        if (!used.get()) {
            return;
        }

        List<PlayerProfile> targetPlayers = getRandomSetOfPlayers();

        CommandBook.server().getScheduler().runTaskAsynchronously(
                CommandBook.inst(),
                () -> updatePlayersTo(targetPlayers)
        );
    }

    private List<PlayerProfile> getRandomSetOfPlayers() {
        OfflinePlayer[] offlinePlayers = CommandBook.server().getOfflinePlayers();

        HashMap<UUID, PlayerProfile> targetPlayers = new HashMap<>();
        while (targetPlayers.size() < Math.min(offlinePlayers.length, config.numberOfPlayers)) {
            OfflinePlayer offlinePlayer = CollectionUtil.getElement(offlinePlayers);
            targetPlayers.put(
                    offlinePlayer.getUniqueId(),
                    Bukkit.createProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName())
            );
        }

        return List.copyOf(targetPlayers.values());
    }

    public void updatePlayersTo(List<PlayerProfile> playerProfiles) {
        for (PlayerProfile profile : playerProfiles) {
            profile.complete();

            Validate.isTrue(profile.hasTextures());
        }

        randomizedProfilesLock.writeLock().lock();

        try {
            randomizedProfiles = playerProfiles;
        } finally {
            randomizedProfilesLock.writeLock().unlock();
        }
    }

    public List<PlayerProfile> getPlayerProfiles() {
        randomizedProfilesLock.readLock().lock();

        try {
            if (!used.get()) {
                used.set(true);
            }

            return randomizedProfiles;
        } finally {
            randomizedProfilesLock.readLock().unlock();
        }
    }

    public Optional<ItemStack> getRandomSkull() {
        List<PlayerProfile> playerProfiles = getPlayerProfiles();
        if (playerProfiles.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(ItemUtil.makeSkull(CollectionUtil.getElement(playerProfiles)));
    }
}
