/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet;

import com.sk89q.commandbook.command.argument.OfflineSinglePlayerTarget;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.math.BigDecimal;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class WalletCommands {
    private final WalletComponent component;

    public WalletCommands(WalletComponent component) {
        this.component = component;
    }

    @Command(name = "balance", desc = "View balance.")
    @CommandPermissions({"aurora.wallet.balance"})
    public void moneyCmd(Player player) {
        component.getBalance(player).thenAccept(
            (balance) -> {
                ChatUtil.sendNotice(player, "You have ", component.format(balance), ".");
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
    }

    @Command(name = "pay", desc = "Pay another player.")
    @CommandPermissions({"aurora.wallet.pay"})
    public void moneyPayCmd(Player player,
                            @Arg(desc = "target") OfflineSinglePlayerTarget target,
                            @Arg(desc = "amount") double amount) {

        BigDecimal amountBigDec = new BigDecimal(amount);
        component.removeFromBalance(player, amountBigDec).thenApplyFailableAsynchronously(
            TaskResult::fromCondition,
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        ).thenComposeFailableAsynchronously(
            (ignored) -> {
                return component.addToBalance(target.get(), amountBigDec);
            },
            (ignored) -> {
                ChatUtil.sendError(player, "You don't have enough ", component.currencyNamePlural() + ".");
            }
        ).thenAcceptAsynchronously(
            (newBalance) -> {
                OfflinePlayer targetOfflinePlayer = target.get();
                Text amountMsg = component.format(amountBigDec);
                ChatUtil.sendNotice(player, "Paid ", amountMsg, " to ", targetOfflinePlayer.getName(), ".");
                if (targetOfflinePlayer.isOnline()) {
                    Player receiver = targetOfflinePlayer.getPlayer();
                    ChatUtil.sendNotice(receiver, "Received ", amountMsg, " from ", player.getName(), ".");
                }
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
    }
}
