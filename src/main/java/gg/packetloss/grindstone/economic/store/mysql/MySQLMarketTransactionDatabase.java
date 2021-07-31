/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.data.MySQLPreparedStatement;
import gg.packetloss.grindstone.economic.store.ItemTransaction;
import gg.packetloss.grindstone.economic.store.MarketItemInfo;
import gg.packetloss.grindstone.economic.store.MarketTransactionDatabase;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static gg.packetloss.grindstone.economic.store.mysql.MarketDatabaseHelper.MARKET_INFO_COLUMNS;
import static gg.packetloss.grindstone.economic.store.mysql.MarketDatabaseHelper.getMarketItem;

public class MySQLMarketTransactionDatabase implements MarketTransactionDatabase {

    private final Queue<MySQLPreparedStatement> queue = new LinkedList<>();

    @Override
    public boolean load() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String tranSQL = "CREATE TABLE IF NOT EXISTS `market-transactions` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`date` DATETIME NOT NULL," +
                    "`player` INT NOT NULL," +
                    "`item` INT NOT NULL," +
                    "`amount` INT NOT NULL," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM;";
            try (PreparedStatement statement = connection.prepareStatement(tranSQL)) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean save() {
        if (queue.isEmpty()) return true;
        try (Connection connection = MySQLHandle.getConnection()) {
            connection.setAutoCommit(false);
            while (!queue.isEmpty()) {
                MySQLPreparedStatement row = queue.poll();
                row.setConnection(connection);
                row.executeStatements();
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void logTransactionCommon(Player player, MarketTransactionLine transactionLine, boolean purchase) {
        int amount = transactionLine.getAmount();

        try {
            int internalPlayerID = MySQLHandle.getPlayerInternalID(player.getUniqueId()).get();
            int itemID = MySQLItemStoreDatabase.getItemID(transactionLine.getItem().getName());
            ItemTransactionStatement transaction = new ItemTransactionStatement(
                    internalPlayerID, itemID, (purchase ? amount : -amount)
            );
            try (Connection connection = MySQLHandle.getConnection()) {
                transaction.setConnection(connection);
                transaction.executeStatements();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logPurchaseTransaction(Player player, MarketTransactionLine transactionLine) {
        logTransactionCommon(player, transactionLine, true);
    }

    @Override
    public void logSaleTransaction(Player player, MarketTransactionLine transactionLine) {
        logTransactionCommon(player, transactionLine, false);

    }

    @Override
    public List<ItemTransaction> getTransactions() {
        return getTransactions(null, null);
    }

    private String getTransactionFilteringSQL(String columns, String itemName, UUID playerID) {
        String sql = "SELECT " + columns + " FROM `market-transactions`"
            + "INNER JOIN `lb-players` ON `market-transactions`.`player` = `lb-players`.`playerid`"
            + "INNER JOIN `market-items` ON `market-items`.`id` = `market-transactions`.`item`";
        if (itemName != null) {
            sql += "WHERE `market-items`.`name` = \'" + itemName + "\'";
        }
        if (playerID != null) {
            if (itemName != null) sql += "AND";
            else sql += "WHERE";
            sql += "`lb-players`.`uuid` = \'" + playerID + "\'";
        }
        return sql;
    }

    private Map<Integer, MarketItemInfo> getAffectedItems(Connection connection, String itemName, UUID playerID) throws SQLException {
        Map<Integer, MarketItemInfo> itemMap = new HashMap<>();

        String sql = getTransactionFilteringSQL("DISTINCT `market-items`.`id`, " + MARKET_INFO_COLUMNS, itemName, playerID);
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
    public List<ItemTransaction> getTransactions(String itemName, UUID playerID) {
        List<ItemTransaction> transactions = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            Map<Integer, MarketItemInfo> lookupMap = getAffectedItems(connection, itemName, playerID);

            String sql = getTransactionFilteringSQL("`lb-players`.`playername`, `market-items`.`id`, `market-transactions`.`amount`", itemName, playerID);
            sql += "ORDER BY `market-transactions`.`date` DESC";
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
