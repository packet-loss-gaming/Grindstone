package gg.packetloss.grindstone.city.engine.pixieitems;

import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.minecraft.util.commands.CommandException;
import org.apache.commons.lang3.Validate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

class PixieCommandSession extends PersistentSession {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private PixieCommand command = PixieCommand.NOTHING;
    private int networkID = -1;

    protected PixieCommandSession() {
        super(MAX_AGE);
    }

    public PixieCommand getCurrentCommand() {
        return command;
    }

    public void setCommandAction(PixieCommand commandAction) throws CommandException {
        if (networkID == -1) {
            throw new CommandException("No network currently selected!");
        }
        this.command = commandAction;
    }

    public void performedAction() {
        this.command = PixieCommand.NOTHING;
    }

    public void setCurrentNetwork(int networkID) {
        Validate.isTrue(networkID >= 0);
        this.networkID = networkID;
    }

    public Optional<Integer> getCurrentNetworkID() {
        return networkID == -1 ? Optional.empty() : Optional.of(networkID);
    }
}
