/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

/**
 * Nothing to see here move along
 *
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Cleansweep", desc = "Cleanup")
public class CleansweepComponent extends BukkitComponent implements Listener {

    private CommandBook inst = CommandBook.inst();
    private Logger log = inst.getLogger();
    private Server server = CommandBook.server();

    @Override
    public void enable() {

        //inst.registerEvents(this);
    }

    /*
     *
     * Cleans out all containers
     *
     * WARNING: THIS CODE IS EXTREMELY DANGEROUS AND WILL CLEAR WITHOUT DISCRIMINATION
     *
     *
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        for (BlockState bState : event.getChunk().getTileEntities()) {
            if (bState instanceof Chest) {
                ((Chest) bState).getInventory().clear();
                bState.update(true);
            }

            if (bState instanceof Dispenser) {
                ((Dispenser) bState).getInventory().clear();
                bState.update(true);
            }

            if (bState instanceof Furnace) {
                ((Furnace) bState).getInventory().clear();
                bState.update(true);
            }

            if (bState instanceof BrewingStand) {
                ((BrewingStand) bState).getInventory().clear();
                bState.update(true);
            }
        }
    }
    */
}