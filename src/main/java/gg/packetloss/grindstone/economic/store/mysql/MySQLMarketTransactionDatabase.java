/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.data.MySQLPreparedStatement;
import gg.packetloss.grindstone.economic.store.ItemTransaction;
import gg.packetloss.grindstone.economic.store.MarketTransactionDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MySQLMarketTransactionDatabase implements MarketTransactionDatabase {

    private Queue<MySQLPreparedStatement> queue = new LinkedList<>();

    @Override
    public boolean load() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String tranSQL = "CREATE TABLE IF NOT EXISTS `market-transactions` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`date` DATETIME NOT NULL," +
                    "`player` INT NOT NULL," +
                    "`item` INT NOT NULL," +
                    "`stock` INT NOT NULL DEFAULT 0," +
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

    @Override
    public void logTransaction(String playerName, String itemName, int amount) {
        try {
            int playerID = MySQLHandle.getPlayerId(playerName);
            int itemID = MySQLItemStoreDatabase.getItemID(itemName);
            ItemTransactionStatement transaction = new ItemTransactionStatement(playerID, itemID, amount);
            try (Connection connection = MySQLHandle.getConnection()) {
                transaction.setConnection(connection);
                transaction.executeStatements();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<ItemTransaction> getTransactions() {
        return getTransactions(null, null);
    }

    @Override
    public List<ItemTransaction> getTransactions(String itemName, String playerName) {
        List<ItemTransaction> transactions = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `lb-players`.`playername`, `market-items`.`name`, `market-transactions`.`amount` "
                    + "FROM `market-transactions`"
                    + "INNER JOIN `lb-players` ON `market-transactions`.`player` = `lb-players`.`playerid`"
                    + "INNER JOIN `market-items` ON `market-items`.`id` = `market-transactions`.`item`";
            if (itemName != null) {
                sql += "WHERE `market-items`.`name` = \'" + itemName + "\'";
            }
            if (playerName != null) {
                if (itemName != null) sql += "AND";
                else sql += "WHERE";
                sql += "`lb-players`.`playername` = \'" + playerName + "\'";
            }
            sql += "ORDER BY `market-transactions`.`date` DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        transactions.add(new ItemTransaction(
                                results.getString(1),
                                results.getString(2),
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
