package com.skelril.aurora;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Pumpkin Scuba", desc = "Breath underwater.")
public class PumpkinScubaHelmetComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 40);
    }

    @Override
    public void run() {

        for (Player player : server.getOnlinePlayers()) {
            if (player.getInventory().getHelmet() != null
                    && player.getInventory().getHelmet().getTypeId() == BlockID.PUMPKIN) {
                if (player.getRemainingAir() != player.getMaximumAir()) {
                    player.setRemainingAir(player.getMaximumAir());
                }
            }
        }
    }
}
