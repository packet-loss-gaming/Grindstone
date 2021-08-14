/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.economic.store.MarketItemInfo;

import java.sql.ResultSet;
import java.sql.SQLException;

class MarketDatabaseHelper {
    public static final String MARKET_INFO_COLUMNS = "`market-items`.`name`, " +
        "`market-items`.`price`, " +
        "`market-items`.`current-price`, " +
        "`market-items`.`stock`, " +
        "`market-items`.`buyable`, " +
        "`market-items`.`sellable`";

    public static MarketItemInfo getMarketItem(ResultSet results, int startingIndex) throws SQLException {
        return new MarketItemInfo(
            results.getString(startingIndex),
            results.getDouble(startingIndex + 1),
            results.getDouble(startingIndex + 2),
            results.getInt(startingIndex + 3),
            !results.getBoolean(startingIndex + 4),
            !results.getBoolean(startingIndex + 5)
        );
    }
}
