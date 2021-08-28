/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems;

import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

@CommandContainer
public class ItemSorterAddCommands {
    private final SessionComponent sessions;

    public ItemSorterAddCommands(SessionComponent sessions) {
        this.sessions = sessions;
    }

    @Command(name = "source", desc = "Add a source to the network")
    public void addSourceCmd(Player owner) throws CommandException {
        PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);
        session.commandToAddSource();

        ChatUtil.sendNotice(owner, "Punch the chest you'd like to make a source.");
    }

    @Command(name = "sink", desc = "Add a sink to the network")
    public void addSinkCmd(Player owner,
                           @Arg(desc = "network name", def = "overwrite") String mode) throws CommandException {
        PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);

        String variant = mode.toUpperCase();
        try {
            session.commandToAddSink(PixieSinkVariant.valueOf(variant));
        } catch (IllegalArgumentException ex) {
            throw new CommandException("Valid modes are: overwrite, add, void");
        }

        ChatUtil.sendNotice(owner, "Punch the container you'd like to make a sink.");
    }
}
