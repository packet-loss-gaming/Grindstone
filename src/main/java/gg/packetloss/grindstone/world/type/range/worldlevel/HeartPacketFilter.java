/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedParticle;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.Particle;

public class HeartPacketFilter extends PacketAdapter {
    private static final int PARTICLE_COUNT_INDEX = 0;

    private final WorldLevelComponent parent;

    public HeartPacketFilter(WorldLevelComponent parent) {
        super(CommandBook.inst(), ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_PARTICLES);
        this.parent = parent;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        for (WrappedParticle value : packet.getNewParticles().getValues()) {
            if (value.getParticle() == Particle.DAMAGE_INDICATOR && parent.sourceDamageLevel > 0) {
                int numHeartParticles = packet.getIntegers().read(PARTICLE_COUNT_INDEX);
                int newNumHeartParticles = (int) Math.max(
                    1,
                    parent.scaleHealth(numHeartParticles, 100, parent.sourceDamageLevel)
                );
                packet.getIntegers().write(PARTICLE_COUNT_INDEX, newNumHeartParticles);
                break;
            }
        }
    }
}
