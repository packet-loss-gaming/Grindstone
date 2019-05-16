/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.data.MySQLPreparedStatement;
import gg.packetloss.grindstone.economic.store.ItemPricePair;
import gg.packetloss.grindstone.economic.store.ItemStoreDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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
    public void addItem(String playerName, String itemName, double price, boolean disableBuy, boolean disableSell) {
        queue.add(new ItemRowStatement(itemName, price, !disableBuy, !disableSell));
    }

    @Override
    public void removeItem(String playerName, String itemName) {
        queue.add(new ItemDeleteStatement(itemName));
    }

    public static int getItemID(String name) throws SQLException {
        try (Connection connection = MySQLHandle.getConnection()) {
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

    public static String getItemName(int id) throws SQLException {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `name` FROM `market-items` WHERE `id` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return results.getString(1);
                    }
                }
            }
        }
        return null;
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

    // FIXME: This should be in a more general place.

    private String preparePlaceHolders(int length) {
        return String.join(",", Collections.nCopies(length, "?"));
    }

    private void setValues(PreparedStatement preparedStatement, List<String> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            preparedStatement.setString(i + 1, values.get(i));
        }
    }

    @Override
    public Map<String, ItemPricePair> getItems(Collection<String> names) {
        Map<String, ItemPricePair> nameItemMapping = new HashMap<>();

        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` IN (" + preparePlaceHolders(names.size()) + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                List<String> uppercaseNames = names.stream().map(String::toUpperCase).collect(Collectors.toList());
                setValues(statement, uppercaseNames);

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        nameItemMapping.put(
                            results.getString(1).toUpperCase(),
                            new ItemPricePair(
                                results.getString(1),
                                results.getDouble(2),
                                !results.getBoolean(3),
                                !results.getBoolean(4)
                            )
                        );
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nameItemMapping;
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
