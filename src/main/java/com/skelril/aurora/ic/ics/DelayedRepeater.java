/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.ic.ics;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.*;
import org.bukkit.Server;

import java.util.logging.Logger;

public class DelayedRepeater extends AbstractIC {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private long delay;

    public DelayedRepeater(Server server, ChangedSign block, ICFactory factory) {

        super(server, block, factory);
    }

    @Override
    public void load() {
        try {
            delay = Long.parseLong(getLine(2));
        } catch (Exception ex) {
            delay = 20;
        }
    }

    @Override
    public String getTitle() {

        return "Delay Repeater";
    }

    @Override
    public String getSignTitle() {

        return "DELAY REPEATER";
    }

    @Override
    public void trigger(final ChipState chip) {

        final boolean trigger = chip.getInput(0);
        server.getScheduler().runTaskLater(inst, () -> chip.setOutput(0, trigger), delay);
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new DelayedRepeater(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Delays a current by x ticks.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[]{"delay", ""};
        }
    }
}
