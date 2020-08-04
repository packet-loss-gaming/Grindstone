/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.garbagemule.MobArena.events.ArenaPlayerLeaveEvent;
import com.garbagemule.MobArena.framework.Arena;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.events.guild.GuildGrantExpEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.IOException;


@ComponentInformation(friendlyName = "MACL", desc = "Mob Arena Compatibility Layer")
@Depend(plugins = {"MobArena"}, components = {PlayerStateComponent.class, GuildComponent.class})
public class MobArenaCLComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private PlayerStateComponent playerState;
    @InjectComponent
    private GuildComponent guilds;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    public boolean isInArena(Player player) {
        return playerState.hasValidStoredState(PlayerStateKind.MOB_ARENA_COMPATIBILITY, player);
    }

    private boolean isGuildArena(Arena arena) {
        return arena.arenaName().toLowerCase().contains("guild");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaEnter(ArenaPlayerJoinEvent event) {
        Player player = event.getPlayer();

        try {
            playerState.pushState(PlayerStateKind.MOB_ARENA_COMPATIBILITY, player);
            if (!isGuildArena(event.getArena())) {
                guilds.getState(player).ifPresent(GuildState::disablePowers);
            }
        } catch (IOException | ConflictingPlayerStateException e) {
            e.printStackTrace();

            ChatUtil.sendError(player, "Failed to clear temporary state.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaLeave(ArenaPlayerLeaveEvent event) {
        Player player = event.getPlayer();

        try {
            playerState.popState(PlayerStateKind.MOB_ARENA_COMPATIBILITY, player);
        } catch (IOException e) {
            e.printStackTrace();

            ChatUtil.sendError(player, "Mob arena integration is failing. Please report this error.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerTriggerEvent event) {
        if (!isInArena(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onGuildExp(GuildGrantExpEvent event) {
        if (!isInArena(event.getPlayer())) {
            return;
        }

        double baseExp = event.getGrantedExp();

        // Cap the xp doubling to +200 "just in case"
        double targetExp = Math.min(baseExp + 200, baseExp * 2);
        event.setGrantedExp(targetExp);
    }
}
