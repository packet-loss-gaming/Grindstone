/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;

@ComponentInformation(friendlyName = "Packet Interception", desc = "Intercept and modify packets.")
public class PacketInterceptionComponent extends BukkitComponent {
    private ProtocolManager protocolManager;

    @Override
    public void enable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void addListener(PacketAdapter adapter) {
        protocolManager.addPacketListener(adapter);
    }
}
