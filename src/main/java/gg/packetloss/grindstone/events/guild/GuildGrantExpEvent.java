/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import gg.packetloss.grindstone.guild.GuildType;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class GuildGrantExpEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final GuildType guild;
    private double grantedExp;
    private boolean cancelled = false;

    public GuildGrantExpEvent(Player who, GuildType type, double grantedExpr) {
        super(who);
        this.guild = type;
        setGrantedExp(grantedExpr);
    }

    public GuildType getGuild() {
        return guild;
    }

    public double getGrantedExp() {
        return grantedExp;
    }

    public void setGrantedExp(double grantedExp) {
        Validate.isTrue(grantedExp >= 0);
        this.grantedExp = grantedExp;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
