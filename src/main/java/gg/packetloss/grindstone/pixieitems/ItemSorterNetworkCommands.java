/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;
import gg.packetloss.grindstone.pixieitems.manager.PixieNetworkManager;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;

import java.util.Collections;
import java.util.Optional;

@CommandContainer
public class ItemSorterNetworkCommands {
    private final SessionComponent sessions;
    private final PixieNetworkManager manager;

    public ItemSorterNetworkCommands(SessionComponent sessions, PixieNetworkManager manager) {
        this.sessions = sessions;
        this.manager = manager;
    }

    @Command(name = "create", aliases = {"add"}, desc = "Create a new sorter system")
    public void createCmd(Player owner, @Arg(desc = "network name") String name) {
        String upperName = name.toUpperCase();

        manager.createNetwork(owner.getUniqueId(), upperName, owner.getLocation()).thenAccept((optNetworkDetail) -> {
            if (optNetworkDetail.isEmpty()) {
                ChatUtil.sendError(owner, "Failed to create network!");
                return;
            }

            ChatUtil.sendNotice(owner, "New item sorter network '" + upperName + "' created!");

            PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);
            session.setCurrentNetwork(optNetworkDetail.get());
        });
    }

    @Command(name = "use", aliases = {"select"}, desc = "Work with an existing sorter system")
    public void useCmd(Player owner, @Arg(desc = "network name") String name) {
        String upperName = name.toUpperCase();

        manager.selectNetwork(owner.getUniqueId(), upperName).thenAccept((optNetworkDetail) -> {
            if (optNetworkDetail.isEmpty()) {
                ChatUtil.sendError(owner, "Failed to find an item sorter network by that name!");
                return;
            }

            ChatUtil.sendNotice(owner, "Item sorter network '" + upperName + "' selected!");

            PixieCommandSession session = sessions.getSession(PixieCommandSession.class, owner);
            session.setCurrentNetwork(optNetworkDetail.get());
        });
    }

    @Command(name = "list", desc = "List sorter networks")
    public void listNetworksCmd(Player owner,
                                @ArgFlag(name = 'p', desc = "page", def = "1") int page) {
        manager.selectNetworks(owner.getUniqueId()).thenAcceptAsynchronously((networks) -> {
            Collections.sort(networks);
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                new TextComponentChatPaginator<PixieNetworkDetail>(ChatColor.GOLD, "Networks") {
                    @Override
                    public Optional<String> getPagerCommand(int page) {
                        return Optional.of("//sorter network list -p " + page);
                    }

                    @Override
                    public Text format(PixieNetworkDetail network) {
                        return Text.of(
                            ChatColor.BLUE,
                            network.getName(),
                            TextAction.Hover.showText(Text.of("Use ", network.getName()," network")),
                            TextAction.Click.runCommand("//sorter network use " + network.getName())
                        );
                    }
                }.display(owner, networks, page);
            });
        });
    }
}
