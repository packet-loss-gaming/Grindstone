package gg.packetloss.grindstone;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Packet Interception", desc = "Intercept and modify packets.")
public class PacketInterceptionComponent extends BukkitComponent {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private ProtocolManager protocolManager;

    @Override
    public void enable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void addListener(PacketAdapter adapter) {
        protocolManager.addPacketListener(adapter);
    }
}
