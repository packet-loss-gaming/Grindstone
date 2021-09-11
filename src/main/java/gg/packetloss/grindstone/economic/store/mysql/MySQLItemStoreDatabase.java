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
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import gg.packetloss.grindstone.util.ChanceUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.economic.store.MarketComponent.LOWER_MARKET_LOSS_THRESHOLD;
import static gg.packetloss.grindstone.economic.store.mysql.MarketDatabaseHelper.MARKET_INFO_COLUMNS;
import static gg.packetloss.grindstone.economic.store.mysql.MarketDatabaseHelper.getMarketItem;
import static gg.packetloss.grindstone.util.DBUtil.preparePlaceHolders;
import static gg.packetloss.grindstone.util.DBUtil.setStringValues;

public class MySQLItemStoreDatabase implements ItemStoreDatabase {
    private final Queue<MySQLPreparedStatement> queue = new LinkedList<>();

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

    private static final int X_DIVISOR = 50;

    private int getMaxAutoStock(double baseValue) {
        baseValue = Math.round(baseValue + X_DIVISOR);
        return (int) (10000 / (baseValue / X_DIVISOR));
    }

    private int getNewStock(double baseValue, int existingStock) {
        int maxAutoStock = getMaxAutoStock(baseValue);
        int restockAmount = ChanceUtil.getRangedRandom(0, Math.max(1, maxAutoStock / 10));
        int restockedStock = Math.min(maxAutoStock, existingStock + restockAmount);

        return Math.max(existingStock, restockedStock);
    }

    private int applyStockNoise(double baseValue, int newStock) {
        int maxAutoStock = getMaxAutoStock(baseValue);

        // Inject:
        //  - 10% noise when more than 70% full and probability selects it
        //  - 10% noise when 100% full
        boolean chanceNoise = newStock > (maxAutoStock * .7) && ChanceUtil.getChance(3);
        boolean overfull = newStock >= maxAutoStock;

        if (chanceNoise || overfull) {
            newStock = (int) (newStock * ChanceUtil.getRangedRandom(.9, 1d));
        }

        return newStock;
    }

    private int getNewStock(double baseValue, int existingStock, int restockingRounds) {
        // If we have something in the lower market loss threshold, it's self managed.
        if (baseValue >= LOWER_MARKET_LOSS_THRESHOLD) {
            return existingStock;
        }

        for (int i = 0; i < restockingRounds; ++i) {
            existingStock = getNewStock(baseValue, existingStock);
            existingStock = applyStockNoise(baseValue, existingStock);
        }

        return existingStock;
    }

    private double getNewValue(double baseValue, int newStock) {
        int targetStock = Math.max(10, getMaxAutoStock(baseValue));
        double percentOutOfStock = Math.max(0d, targetStock - newStock) / targetStock;

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
    public void addItem(String itemName, double price, boolean disableBuy, boolean disableSell) {
        queue.add(new ItemRowStatement(itemName, price, !disableBuy, !disableSell));
    }

    @Override
    public void removeItem(String itemName) {
        queue.add(new ItemDeleteStatement(itemName));
    }

    private void adjustStocksCommon(List<MarketTransactionLine> transactionLines, boolean purchase) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String updateSql = "UPDATE `market-items` SET `stock` = `stock` + ? WHERE `name` = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                for (MarketTransactionLine transactionLine : transactionLines) {
                    String itemName = transactionLine.getItem().getName();
                    int amount = transactionLine.getAmount();

                    updateStatement.setInt(1, (purchase ? -amount : amount));
                    updateStatement.setString(2, itemName);

                    updateStatement.addBatch();
                }

                updateStatement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void adjustStocksForBuy(List<MarketTransactionLine> transactionLines) {
        adjustStocksCommon(transactionLines, true);
    }

    @Override
    public void adjustStocksForSale(List<MarketTransactionLine> transactionLines) {
        adjustStocksCommon(transactionLines, false);
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

    @Override
    public MarketItemInfo getItem(String name) {
        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT " + MARKET_INFO_COLUMNS + " FROM `market-items` WHERE `name` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name.toUpperCase());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return getMarketItem(results, 1);
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
            String sql = "SELECT " + MARKET_INFO_COLUMNS + " FROM `market-items` WHERE `name` IN (" + preparePlaceHolders(names.size()) + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                List<String> uppercaseNames = names.stream().map(String::toUpperCase).collect(Collectors.toList());
                setStringValues(statement, uppercaseNames);

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        nameItemMapping.put(
                            results.getString(1).toUpperCase(),
                            getMarketItem(results, 1)
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
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + MARKET_INFO_COLUMNS + " FROM `market-items`")) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        items.add(getMarketItem(results, 1));
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
            String sql = "SELECT " + MARKET_INFO_COLUMNS + " FROM `market-items` WHERE SUBSTRING_INDEX(`name`, ':', -1) LIKE ?";
            if (!showHidden) {
                sql += " AND (`buyable` = true OR `sellable` = true)";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "%" + filter.replaceAll("\\s+", "_") + "%");
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        items.add(getMarketItem(results, 1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}
