/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems;

import com.sk89q.commandbook.component.session.PersistentSession;
import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;
import org.apache.commons.lang.Validate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

class PixieCommandSession extends PersistentSession {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private PixieCommand command = PixieCommand.NOTHING;
    private PixieSinkCreationMode sinkCreationMode = null;
    private PixieNetworkDetail network = null;

    protected PixieCommandSession() {
        super(MAX_AGE);
    }

    public PixieCommand getCurrentCommand() {
        return command;
    }

    public PixieSinkCreationMode getSinkCreationMode() {
        Validate.notNull(sinkCreationMode);
        return sinkCreationMode;
    }

    private void resetCommandData() {
        this.command = PixieCommand.NOTHING;
        this.sinkCreationMode = null;
    }

    private void setCommandAction(PixieCommand commandAction) throws CommandException {
        if (network == null) {
            throw new CommandException("No network currently selected!");
        }

        resetCommandData();

        this.command = commandAction;
    }

    public void commandToAddSource() throws CommandException {
        setCommandAction(PixieCommand.ADD_SOURCE);
    }

    public void commandToAddSink(PixieSinkCreationMode variant) throws CommandException {
        setCommandAction(PixieCommand.ADD_SINK);
        sinkCreationMode = variant;
    }

    public void performedAction() {
        resetCommandData();
    }

    public void setCurrentNetwork(PixieNetworkDetail network) {
        this.network = network;
    }

    public Optional<PixieNetworkDetail> getCurrentNetwork() {
        return Optional.ofNullable(network);
    }
}
