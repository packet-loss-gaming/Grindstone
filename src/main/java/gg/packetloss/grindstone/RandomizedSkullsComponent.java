/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ComponentInformation(friendlyName = "Randomized Skulls", desc = "Get a random subset of skulls for players")
@Depend(components = {ProfanityComponent.class})
public class RandomizedSkullsComponent extends BukkitComponent implements Runnable {
    private final ReadWriteLock randomizedProfilesLock = new ReentrantReadWriteLock();
    private final AtomicInteger timesUsed = new AtomicInteger(0);
    private final AtomicBoolean beingUpdated = new AtomicBoolean(false);
    private List<PlayerProfile> randomizedProfiles = List.of();

    @InjectComponent
    private ProfanityComponent profanity;
    
    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        // Create an initial set blocking, then schedule updates every hour
        updatePlayersTo(getRandomSetOfPlayers());
        Bukkit.getScheduler().runTaskTimer(
                CommandBook.inst(),
                this,
                TimeUtil.convertSecondsToTicks(config.updateFrequency),
                TimeUtil.convertSecondsToTicks(config.updateFrequency)
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
        @Setting("number-before-update")
        public int numberBeforeUpdate = 8;
        @Setting("update-check-frequency")
        public int updateFrequency = 60;
    }

    @Override
    public void run() {
        // Only update if someone has seen these skulls
        if (timesUsed.get() < config.numberBeforeUpdate) {
            return;
        }

        if (beingUpdated.get()) {
            return;
        }

        beingUpdated.set(true);
        List<PlayerProfile> targetPlayers = getRandomSetOfPlayers();

        PluginTaskExecutor.submitAsync(() -> updatePlayersTo(targetPlayers));
    }
    
    private boolean hasUnacceptableUsername(OfflinePlayer player) {
        // We don't know their name, assume the worst.
        if (player.getName() == null) {
            return true;
        }
        
        if (profanity.containsCensoredWord(player.getName())) {
            return true;
        }

        return false;
    }

    private List<PlayerProfile> getRandomSetOfPlayers() {
        OfflinePlayer[] offlinePlayers = CommandBook.server().getOfflinePlayers();

        HashMap<UUID, PlayerProfile> targetPlayers = new HashMap<>();
        for (int i = 0; i < offlinePlayers.length * 2 && targetPlayers.size() != config.numberOfPlayers; ++i) {
            OfflinePlayer offlinePlayer = CollectionUtil.getElement(offlinePlayers);
            if (hasUnacceptableUsername(offlinePlayer)) {
                continue;
            }
            
            targetPlayers.put(
                    offlinePlayer.getUniqueId(),
                    Bukkit.createProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName())
            );
        }

        return List.copyOf(targetPlayers.values());
    }

    public void updatePlayersTo(List<PlayerProfile> playerProfiles) {
        try {
            for (PlayerProfile profile : playerProfiles) {
                profile.complete();

                Validate.isTrue(profile.hasTextures());
            }
        } catch (Throwable t) {
            // Sometimes Mojang's auth servers are unreliable which causes this to fail
            t.printStackTrace();
            return;
        }

        randomizedProfilesLock.writeLock().lock();

        try {
            randomizedProfiles = playerProfiles;
            timesUsed.set(0);
            beingUpdated.set(false);
        } finally {
            randomizedProfilesLock.writeLock().unlock();
        }
    }

    public List<PlayerProfile> getPlayerProfiles() {
        randomizedProfilesLock.readLock().lock();

        try {
            if (!beingUpdated.get()) {
                timesUsed.getAndIncrement();
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
