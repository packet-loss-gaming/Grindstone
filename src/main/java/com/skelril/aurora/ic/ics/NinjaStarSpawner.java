/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.ic.ics;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.*;
import com.sk89q.craftbook.util.LocationUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;

import java.util.logging.Logger;

public class NinjaStarSpawner extends AbstractSelfTriggeredIC {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private int quantity;

    public NinjaStarSpawner(Server server, ChangedSign block, ICFactory factory) {

        super(server, block, factory);
    }

    @Override
    public void load() {

        try {
            quantity = Integer.parseInt(getSign().getLine(2));
        } catch (NumberFormatException ex) {
            quantity = 1;
        }
    }

    @Override
    public String getTitle() {

        return "Star Spawner";
    }

    @Override
    public String getSignTitle() {

        return "STAR SPAWNER";
    }

    @Override
    public void trigger(ChipState chip) {

        drop();
    }

    @Override
    public void think(ChipState chip) {

        if (!chip.getInput(0)) {
            trigger(chip);
        }
    }

    public void drop() {

        Location k = LocationUtil.getCenterOfBlock(LocationUtil.getNextFreeSpace(getBackBlock(), BlockFace.UP));
        final Item item = k.getWorld().dropItem(k, CustomItemCenter.build(CustomItems.NINJA_STAR, quantity));
        server.getScheduler().runTaskLater(inst, () -> {
            if (item.isValid()) {
                item.remove();
            }
        }, 20 * 15);
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new NinjaStarSpawner(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Spawns Ninja Stars.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[]{"Quantity", ""};
        }
    }
}