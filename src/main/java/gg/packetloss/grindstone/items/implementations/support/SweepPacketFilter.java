package gg.packetloss.grindstone.items.implementations.support;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import static com.comphenix.protocol.wrappers.EnumWrappers.Particle.SWEEP_ATTACK;

public class SweepPacketFilter extends PacketAdapter {
    public SweepPacketFilter() {
        super(CommandBook.inst(), ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_PARTICLES);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        for (EnumWrappers.Particle value : event.getPacket().getParticles().getValues()) {
            if (value == SWEEP_ATTACK) {
                ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
                if (ItemUtil.blocksSweepAttack(heldItem)) {
                    event.setCancelled(true);
                }
                break;
            }
        }
    }
}
