/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet;

import gg.packetloss.grindstone.util.task.promise.FailableTaskFuture;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public interface WalletProvider {
    public FailableTaskFuture<Boolean, Void> hasAtLeast(OfflinePlayer player, BigDecimal amount);

    public FailableTaskFuture<BigDecimal, Void> getBalance(OfflinePlayer player);

    public FailableTaskFuture<BigDecimal, Void> addToBalance(OfflinePlayer player, BigDecimal amount);
    public FailableTaskFuture<Boolean, Void> removeFromBalance(OfflinePlayer player, BigDecimal amount);
    public FailableTaskFuture<Void, Void> setBalance(OfflinePlayer player, BigDecimal amount);
}
