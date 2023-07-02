/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.economic.store.ItemTransaction;
import gg.packetloss.grindstone.economic.store.MarketItemInfo;
import gg.packetloss.grindstone.economic.store.MarketTransactionDatabase;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import static gg.packetloss.grindstone.economic.store.sql.MarketDatabaseHelper.MARKET_INFO_COLUMNS;
import static gg.packetloss.grindstone.economic.store.sql.MarketDatabaseHelper.getMarketItem;

public class SQLMarketTransactionDatabase implements MarketTransactionDatabase {
    private void logTransactionCommon(UUID playerID, Collection<MarketTransactionLine> transactionLines,
                                      boolean purchase) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                INSERT INTO minecraft.market_transactions (date, player_id, item, amount)
                VALUES (
                    ?,
                    (SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1),
                    (SELECT id FROM minecraft.market_items WHERE name = ? LIMIT 1),
                    ?
                )
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (MarketTransactionLine transactionLine : transactionLines) {
                    int amount = transactionLine.getAmount();

                    statement.setTimestamp(1, Timestamp.from(Instant.now()));
                    statement.setString(2, playerID.toString());
                    statement.setString(3, transactionLine.getItem().getName());
                    statement.setInt(4, (purchase ? amount : -amount));
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logPurchaseTransactions(UUID playerID, Collection<MarketTransactionLine> transactionLines) {
        logTransactionCommon(playerID, transactionLines, true);
    }

    @Override
    public void logSaleTransactions(UUID playerID, Collection<MarketTransactionLine> transactionLines) {
        logTransactionCommon(playerID, transactionLines, false);

    }

    private String getFilterLogic(@Nullable String itemName, @Nullable UUID playerID) {
        if (itemName != null && playerID != null) {
            return " WHERE items.name = ? AND players.uuid = ?";
        } else if (itemName != null) {
            return " WHERE items.name = ?";
        } else if (playerID != null) {
            return " WHERE players.uuid = ?";
        }
        return "";
    }

    private Map<Integer, MarketItemInfo> getAffectedItems(Connection connection, @Nullable String itemName,
                                                          @Nullable UUID playerID) throws SQLException {
        Map<Integer, MarketItemInfo> itemMap = new HashMap<>();

        String sql = """
            SELECT DISTINCT items.id, MARKET_INFO_COLUMNS FROM minecraft.market_transactions AS transactions
                JOIN minecraft.players AS players ON players.id = transactions.player_id
                JOIN minecraft.market_items AS items ON items.id = transactions.item
        """.replace("MARKET_INFO_COLUMNS", MARKET_INFO_COLUMNS);
        sql += getFilterLogic(itemName, playerID);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    itemMap.put(results.getInt(1), getMarketItem(results, 2));
                }
            }
        }

        return itemMap;
    }

    @Override
    public List<ItemTransaction> getTransactions(@Nullable String itemName, @Nullable UUID playerID) {
        List<ItemTransaction> transactions = new ArrayList<>();
        try (Connection connection = SQLHandle.getConnection()) {
            Map<Integer, MarketItemInfo> lookupMap = getAffectedItems(connection, itemName, playerID);

            String sql = """
                SELECT players.name, items.id, transactions.amount FROM minecraft.market_transactions AS transactions
                    JOIN minecraft.players AS players ON players.id = transactions.player_id
                    JOIN minecraft.market_items AS items ON items.id = transactions.item
            """;
            sql += getFilterLogic(itemName, playerID);
            sql += " ORDER BY `market-transactions`.`date` DESC";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        transactions.add(new ItemTransaction(
                                results.getString(1),
                                lookupMap.get(results.getInt(2)),
                                results.getInt(3)
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
}
