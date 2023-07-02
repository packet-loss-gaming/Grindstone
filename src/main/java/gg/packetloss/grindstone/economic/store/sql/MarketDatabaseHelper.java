/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.sql;

import gg.packetloss.grindstone.economic.store.MarketItemInfo;

import java.sql.ResultSet;
import java.sql.SQLException;

class MarketDatabaseHelper {
    public static final String MARKET_INFO_COLUMNS = """
        market_items.name,
        market_items.price,
        market_items.current_price,
        market_items.stock,
        market_items.infinite,
        market_items.buyable,
        market_items.sellable
    """;

    public static MarketItemInfo getMarketItem(ResultSet results, int startingIndex) throws SQLException {
        return new MarketItemInfo(
            results.getString(startingIndex),
            results.getBigDecimal(startingIndex + 1),
            results.getBigDecimal(startingIndex + 2),
            results.getInt(startingIndex + 3),
            results.getBoolean(startingIndex + 4),
            !results.getBoolean(startingIndex + 5),
            !results.getBoolean(startingIndex + 6)
        );
    }
}
