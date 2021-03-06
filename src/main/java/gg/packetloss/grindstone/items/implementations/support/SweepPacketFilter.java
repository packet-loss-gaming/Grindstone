/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.support;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedParticle;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

public class SweepPacketFilter extends PacketAdapter {
    public SweepPacketFilter() {
        super(CommandBook.inst(), ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_PARTICLES);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        for (WrappedParticle value : event.getPacket().getNewParticles().getValues()) {
            if (value.getParticle() == Particle.SWEEP_ATTACK) {
                ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
                if (ItemUtil.blocksSweepAttack(heldItem)) {
                    event.setCancelled(true);
                }
                break;
            }
        }
    }
}
