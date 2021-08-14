/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet.database;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletDatabase {
    public BigDecimal getBalance(UUID playerID);
    public BigDecimal addToBalance(UUID playerID, BigDecimal amount);
    public boolean removeFromBalance(UUID playerID, BigDecimal amount);
    public void setBalance(UUID playerID, BigDecimal amount);
}
