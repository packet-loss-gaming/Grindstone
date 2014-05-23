/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.store.mysql;

import com.skelril.aurora.data.MySQLHandle;
import com.skelril.aurora.data.MySQLPreparedStatement;
import com.skelril.aurora.economic.store.ItemPricePair;
import com.skelril.aurora.economic.store.ItemStoreDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MySQLItemStoreDatabase implements ItemStoreDatabase {
    private static final String columns = "`name`, `price`, `buyable`, `sellable`";
    private Queue<MySQLPreparedStatement> queue = new LinkedList<>();

    @Override
    public boolean load() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String mainSQL = "CREATE TABLE IF NOT EXISTS `market-items` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`name` VARCHAR(50) NOT NULL," +
                    "`price` DOUBLE NOT NULL," +
                    "`buyable` TINYINT(1) NOT NULL," +
                    "`sellable` TINYINT(1) NOT NULL," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE INDEX `name` (`name`)" +
                    ") ENGINE=MyISAM;";
            try (PreparedStatement statement = connection.prepareStatement(mainSQL)) {
                statement.executeUpdate();
            }

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
    public void addItem(String playerName, String itemName, double price, boolean disableBuy, boolean disableSell) {
        queue.add(new ItemRowStatement(itemName, price, !disableBuy, !disableSell));
    }

    @Override
    public void removeItem(String playerName, String itemName) {
        queue.add(new ItemDeleteStatement(itemName));
    }

    @Override
    public void logTransaction(String playerName, String itemName, int amount) {
        try {
            int playerID = MySQLHandle.getPlayerId(playerName);
            int itemID = getItemID(itemName);
            ItemTransactionStatement transaction = new ItemTransactionStatement(playerID, itemID, amount);
            try (Connection connection = MySQLHandle.getConnection()) {
                transaction.setConnection(connection);
                transaction.executeStatements();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getItemID(String name) throws SQLException {
        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT `id` FROM `market-items` WHERE `name` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name.toUpperCase());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return results.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public ItemPricePair getItem(String name) {
        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name.toUpperCase());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return new ItemPricePair(
                                results.getString(1),
                                results.getDouble(2),
                                !results.getBoolean(3),
                                !results.getBoolean(4)
                        );
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ItemPricePair> getItemList() {
        List<ItemPricePair> items = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + columns + " FROM `market-items`")) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        items.add(new ItemPricePair(
                                results.getString(1),
                                results.getDouble(2),
                                !results.getBoolean(3),
                                !results.getBoolean(4)
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public List<ItemPricePair> getItemList(String filter, boolean showHidden) {

        if (filter == null || filter.isEmpty()) {
            return getItemList();
        }

        List<ItemPricePair> items = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` LIKE ?";
            if (!showHidden) {
                sql += " AND (`buyable` = true OR `sellable` = true)";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, filter.toUpperCase() + "%");
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        items.add(new ItemPricePair(
                                results.getString(1),
                                results.getDouble(2),
                                !results.getBoolean(3),
                                !results.getBoolean(4)
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}
