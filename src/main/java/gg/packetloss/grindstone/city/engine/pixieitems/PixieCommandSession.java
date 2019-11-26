package gg.packetloss.grindstone.city.engine.pixieitems;

import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.grindstone.city.engine.pixieitems.db.PixieNetworkDetail;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

class PixieCommandSession extends PersistentSession {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private PixieCommand command = PixieCommand.NOTHING;
    private PixieNetworkDetail network = null;

    protected PixieCommandSession() {
        super(MAX_AGE);
    }

    public PixieCommand getCurrentCommand() {
        return command;
    }

    public void setCommandAction(PixieCommand commandAction) throws CommandException {
        if (network == null) {
            throw new CommandException("No network currently selected!");
        }
        this.command = commandAction;
    }

    public void performedAction() {
        this.command = PixieCommand.NOTHING;
    }

    public void setCurrentNetwork(PixieNetworkDetail network) {
        this.network = network;
    }

    public Optional<PixieNetworkDetail> getCurrentNetwork() {
        return Optional.ofNullable(network);
    }
}
