/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player.effects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.entity.Player;

import java.util.List;

public class RiptideEffectHandle {
    private static final byte ENABLE_RIPTIDE = 0x04;
    private static final byte DISABLE_RIPTIDE = 0x00;

    private final Player player;

    public RiptideEffectHandle(Player player) {
        this.player = player;
    }

    private List<WrappedWatchableObject> getMetadataObjects(byte packetVal) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setEntity(player);
        watcher.setObject(8, WrappedDataWatcher.Registry.get(Byte.class), packetVal);
        return watcher.getWatchableObjects();
    }

    private void sendEffectPacket(byte packetVal) {
        PacketContainer packetContainer = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packetContainer.getIntegers().write(0, player.getEntityId());
        packetContainer.getWatchableCollectionModifier().write(0, getMetadataObjects(packetVal));
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(packetContainer, player, true);
    }

    public void enable() {
        sendEffectPacket(ENABLE_RIPTIDE);
    }

    public void disable() {
        sendEffectPacket(DISABLE_RIPTIDE);
    }
}
