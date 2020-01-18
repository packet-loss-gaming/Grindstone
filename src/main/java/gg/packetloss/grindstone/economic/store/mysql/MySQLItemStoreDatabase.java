/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.data.MySQLPreparedStatement;
import gg.packetloss.grindstone.economic.store.ItemStoreDatabase;
import gg.packetloss.grindstone.economic.store.MarketItemInfo;
import gg.packetloss.grindstone.util.ChanceUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.economic.store.MarketComponent.LOWER_MARKET_LOSS_THRESHOLD;
import static gg.packetloss.grindstone.util.DBUtil.preparePlaceHolders;
import static gg.packetloss.grindstone.util.DBUtil.setStringValues;

public class MySQLItemStoreDatabase implements ItemStoreDatabase {
    private static final String columns = "`name`, `price`, `current-price`, `stock`, `buyable`, `sellable`";
    private Queue<MySQLPreparedStatement> queue = new LinkedList<>();

    @Override
    public boolean load() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String mainSQL = "CREATE TABLE IF NOT EXISTS `market-items` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`name` VARCHAR(50) NOT NULL," +
                    "`price` DOUBLE NOT NULL," +
                    "`current-price` DOUBLE NOT NULL," +
                    "`stock` INT NOT NULL DEFAULT 0," +
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

    private int getOrderOfMagnitude(int units) {
        return (int) Math.pow(10, units);
    }

    private int getMaxAutoStock(double baseValue) {
        int digits = 0;

        baseValue = Math.ceil(baseValue);
        while (baseValue >= 1) {
            baseValue /= 10;
            ++digits;
        }

        // worth >= 1000 max auto stock 10    - max restock pace 1
        // worth >=  100 max auto stock 100   - max restock pace 10
        // worth >=   10 max auto stock 1000  - max restock pace 100
        // worth >=    0 max auto stock 10000 - max restock pace 1000

        return getOrderOfMagnitude(Math.max(0, 4 - digits) + 1);
    }

    private int getNewStock(double baseValue, int existingStock) {
        // If we have something in the lower market loss threshold, it's self managed.
        if (baseValue >= LOWER_MARKET_LOSS_THRESHOLD) {
            return existingStock;
        }

        int maxAutoStock = getMaxAutoStock(baseValue);
        int restockAmount = ChanceUtil.getRangedRandom(0, maxAutoStock / 10);
        int restockedStock = Math.min(maxAutoStock, existingStock + restockAmount);

        return Math.max(existingStock, restockedStock);
    }

    private int applyStockNoise(double baseValue, int newStock, boolean massSimulation) {
        int maxAutoStock = getMaxAutoStock(baseValue);

        // Occasionally inject 10% noise when 70% full, or 30% noise, if we're overfull from performing
        // a mass simulation
        boolean chanceNoise = newStock > (maxAutoStock * .7) && ChanceUtil.getChance(3);
        boolean overfull = newStock >= maxAutoStock;

        if (massSimulation && overfull) {
            newStock = (int) (newStock * ChanceUtil.getRangedRandom(.7, 1d));
        } else if (chanceNoise) {
            newStock = (int) (newStock * ChanceUtil.getRangedRandom(.9, 1d));
        }

        return newStock;
    }

    private int getNewStock(double baseValue, int existingStock, int restockingRounds) {
        for (int i = 0; i < restockingRounds; ++i) {
            existingStock = getNewStock(baseValue, existingStock);
        }

        return applyStockNoise(baseValue, existingStock, restockingRounds > 1);
    }

    private double getNewValue(double baseValue, int newStock) {
        int maxAuto = getMaxAutoStock(baseValue);
        double percentOutOfStock = Math.max(0d, maxAuto - newStock) / maxAuto;

        // Use multipliers that result in a net 0, from min - max fluctuation
        double multiplier = .1;
        if (baseValue >= LOWER_MARKET_LOSS_THRESHOLD) {
            multiplier = .04;
        }

        // Inject 30% noise
        multiplier *= ChanceUtil.getRangedRandom(.7d, 1d);

        double change = baseValue * multiplier;

        double minPrice = baseValue - change;
        double maxOverMin = 2 * change;
        return minPrice + (percentOutOfStock * maxOverMin);
    }

    @Override
    public void updatePrices(int restockingRounds) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String updateSql = "UPDATE `market-items` SET `current-price` = ?, `stock` = ? WHERE `id` = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {

                String sql = "SELECT `id`, `price`, `stock` FROM `market-items`";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            int id = results.getInt(1);
                            double price = results.getDouble(2);
                            int stock = results.getInt(3);

                            int newStock = getNewStock(price, stock, restockingRounds);
                            updateStatement.setDouble(1, getNewValue(price, newStock));
                            updateStatement.setInt(2, newStock);
                            updateStatement.setInt(3, id);

                            updateStatement.addBatch();
                        }
                    }
                }

                updateStatement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    public void adjustStocks(Map<String, Integer> adjustments) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String updateSql = "UPDATE `market-items` SET `stock` = `stock` + ? WHERE `name` = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                for (Map.Entry<String, Integer> entry : adjustments.entrySet()) {
                    updateStatement.setInt(1, entry.getValue());
                    updateStatement.setString(2, entry.getKey());

                    updateStatement.addBatch();
                }

                updateStatement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    public MarketItemInfo getItem(String name) {
        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name.toUpperCase());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return new MarketItemInfo(
                                results.getString(1),
                                results.getDouble(2),
                                results.getDouble(3),
                                results.getInt(4),
                                !results.getBoolean(5),
                                !results.getBoolean(6)
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
    public Map<String, MarketItemInfo> getItems(Collection<String> names) {
        Map<String, MarketItemInfo> nameItemMapping = new HashMap<>();

        if (names.isEmpty()) {
            return nameItemMapping;
        }

        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` IN (" + preparePlaceHolders(names.size()) + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                List<String> uppercaseNames = names.stream().map(String::toUpperCase).collect(Collectors.toList());
                setStringValues(statement, uppercaseNames);

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        nameItemMapping.put(
                            results.getString(1).toUpperCase(),
                            new MarketItemInfo(
                                results.getString(1),
                                results.getDouble(2),
                                results.getDouble(3),
                                results.getInt(4),
                                !results.getBoolean(5),
                                !results.getBoolean(6)
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
    public List<MarketItemInfo> getItemList() {
        List<MarketItemInfo> items = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + columns + " FROM `market-items`")) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        items.add(new MarketItemInfo(
                                results.getString(1),
                                results.getDouble(2),
                                results.getDouble(3),
                                results.getInt(4),
                                !results.getBoolean(5),
                                !results.getBoolean(6)
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
    public List<MarketItemInfo> getItemList(String filter, boolean showHidden) {

        if (filter == null || filter.isEmpty()) {
            return getItemList();
        }

        List<MarketItemInfo> items = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT " + columns + " FROM `market-items` WHERE SUBSTRING_INDEX(`name`, ':', -1) LIKE ?";
            if (!showHidden) {
                sql += " AND (`buyable` = true OR `sellable` = true)";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "%" + filter.replaceAll("\\s+", "_") + "%");
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        items.add(new MarketItemInfo(
                                results.getString(1),
                                results.getDouble(2),
                                results.getDouble(3),
                                results.getInt(4),
                                !results.getBoolean(5),
                                !results.getBoolean(6)
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
