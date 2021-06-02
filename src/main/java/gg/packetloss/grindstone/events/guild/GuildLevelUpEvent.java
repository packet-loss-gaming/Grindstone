/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import gg.packetloss.grindstone.guild.GuildType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class GuildLevelUpEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private GuildType guild;
    private int newLevel;

    public GuildLevelUpEvent(Player who, GuildType type, int newLevel) {
        super(who);
        this.guild = type;
        this.newLevel = newLevel;
    }

    public GuildType getGuild() {
        return guild;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}