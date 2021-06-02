/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.playerstate;

import gg.packetloss.grindstone.state.player.PlayerStateKind;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerStatePushEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private PlayerStateKind kind;

    public PlayerStatePushEvent(Player who, PlayerStateKind kind) {
        super(who);
        this.kind = kind;
    }

    public PlayerStateKind getKind() {
        return kind;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
