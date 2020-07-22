/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import gg.packetloss.grindstone.click.ClickType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nullable;

public class DoubleClickEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final ClickType clickType;
    private final Block associatedBlock;
    private final BlockFace associatedBlockFace;

    public DoubleClickEvent(Player who, ClickType clickType,
                            @Nullable Block associatedBlock,
                            @Nullable BlockFace associatedBlockFace) {
        super(who);
        this.clickType = clickType;
        this.associatedBlock = associatedBlock;
        this.associatedBlockFace = associatedBlockFace;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public Block getAssociatedBlock() {
        return associatedBlock;
    }

    public BlockFace getAssociatedBlockFace() {
        return associatedBlockFace;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
