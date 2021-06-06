/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet;

import com.sk89q.commandbook.command.argument.OfflineSinglePlayerTarget;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.math.BigDecimal;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class WalletCommands {
    private WalletComponent component;

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
                ChatUtil.sendNotice(player, "Paid ", target.get().getName(), " ", component.format(amountBigDec), ".");
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
    }

    @Command(name = "grant", desc = "Grant money to a player.")
    @CommandPermissions({"aurora.wallet.admin.grant"})
    public void moneyGrantCmd(Player player,
                              @Arg(desc = "target") OfflineSinglePlayerTarget target,
                              @Arg(desc = "amount") double amount) {

        BigDecimal amountBigDec = new BigDecimal(amount);
        component.addToBalance(target.get(), amountBigDec).thenAcceptAsynchronously(
            (newBalance) -> {
                ChatUtil.sendNotice(player, "Granted ", target.get().getName(), " ", component.format(amountBigDec), ".");
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
    }
}
