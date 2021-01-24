/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet.database;

import gg.packetloss.grindstone.economic.wallet.WalletProvider;
import gg.packetloss.grindstone.util.task.promise.FailableTaskFuture;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.apache.commons.lang.Validate;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public class DatabaseWalletProvider implements WalletProvider {
    private WalletDatabase database;

    public DatabaseWalletProvider(WalletDatabase database) {
        this.database = database;
    }

    @Override
    public FailableTaskFuture<Boolean, Void> hasAtLeast(OfflinePlayer player, BigDecimal amount) {
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) > 0);

        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(database.getBalance(player.getUniqueId()).compareTo(amount) >= 0);
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }

    @Override
    public FailableTaskFuture<BigDecimal, Void> getBalance(OfflinePlayer player) {
        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(database.getBalance(player.getUniqueId()));
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }

    @Override
    public FailableTaskFuture<BigDecimal, Void> addToBalance(OfflinePlayer player, BigDecimal amount) {
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) > 0);
        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(database.addToBalance(player.getUniqueId(), amount));
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }

    @Override
    public FailableTaskFuture<Boolean, Void> removeFromBalance(OfflinePlayer player, BigDecimal amount) {
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) > 0);
        return FailableTaskFuture.asyncTask(() -> {
            try {
                return TaskResult.of(database.removeFromBalance(player.getUniqueId(), amount));
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }

    @Override
    public FailableTaskFuture<Void, Void> setBalance(OfflinePlayer player, BigDecimal amount) {
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) >= 0);

        return FailableTaskFuture.asyncTask(() -> {
            try {
                database.setBalance(player.getUniqueId(), amount);
                return TaskResult.success();
            } catch (Throwable t) {
                t.printStackTrace();
                return TaskResult.failed();
            }
        });
    }
}
