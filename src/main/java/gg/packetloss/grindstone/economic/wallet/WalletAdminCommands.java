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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.math.BigDecimal;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class WalletAdminCommands {
    private final WalletComponent component;

    public WalletAdminCommands(WalletComponent component) {
        this.component = component;
    }

    private void showBalance(CommandSender sender, OfflinePlayer player) {
        component.getBalance(player).thenAcceptAsynchronously(
            (balance) -> {
                ChatUtil.sendNotice(sender, player.getName(), " has ", component.format(balance), ".");
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(sender); }
        );
    }

    @Command(name = "balanceof", desc = "View the balance of a player.")
    @CommandPermissions({"aurora.wallet.admin.view"})
    public void moneyBalanceofCmd(CommandSender sender,
                                  @Arg(desc = "target") OfflineSinglePlayerTarget target) {
        showBalance(sender, target.get());
    }

    @Command(name = "setbalance", desc = "View the balance of a player.")
    @CommandPermissions({"aurora.wallet.admin.manipulate"})
    public void moneySetbalanceCmd(CommandSender sender,
                                   @Arg(desc = "target") OfflineSinglePlayerTarget target,
                                   @Arg(desc = "amount") double amount) {
        BigDecimal amountBigDec = new BigDecimal(amount);
        component.setBalance(target.get(), amountBigDec).thenAcceptAsynchronously(
            (newBalance) -> {
                ChatUtil.sendNotice(
                    sender,
                    "Set balance of ", target.get().getName(), " to ", component.format(amountBigDec), "."
                );
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(sender); }
        );
    }

    @Command(name = "give", desc = "Give money to a player.")
    @CommandPermissions({"aurora.wallet.admin.manipulate"})
    public void moneyGiveCmd(CommandSender sender,
                             @Arg(desc = "target") OfflineSinglePlayerTarget target,
                             @Arg(desc = "amount") double amount) {
        BigDecimal amountBigDec = new BigDecimal(amount);
        component.addToBalance(target.get(), amountBigDec).thenAcceptAsynchronously(
            (newBalance) -> {
                ChatUtil.sendNotice(
                    sender,
                    "Gave ", component.format(amountBigDec), " to ", target.get().getName(), "."
                );
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(sender); }
        );
    }

    @Command(name = "take", desc = "Take money from a player.")
    @CommandPermissions({"aurora.wallet.admin.manipulate"})
    public void moneyTakeCmd(CommandSender sender,
                             @Arg(desc = "target") OfflineSinglePlayerTarget target,
                             @Arg(desc = "amount") double amount) {
        BigDecimal amountBigDec = new BigDecimal(amount);
        component.removeFromBalance(target.get(), amountBigDec).thenApplyFailableAsynchronously(
            TaskResult::fromCondition,
            (ignored) -> { ErrorUtil.reportUnexpectedError(sender); }
        ).thenAcceptAsynchronously(
            (ignored) -> {
                ChatUtil.sendNotice(
                    sender,
                    "Took ", component.format(amountBigDec), " from ", target.get().getName(), "."
                );
            },
            (ignored) -> {
                ChatUtil.sendNotice(sender, "Player doesn't have enough money.");
                showBalance(sender, target.get());
            }
        );
    }
}
