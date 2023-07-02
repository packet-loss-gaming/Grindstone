/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.economic.store.ItemStoreDatabase;
import gg.packetloss.grindstone.economic.store.MarketItemInfo;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import gg.packetloss.grindstone.util.ChanceUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.economic.store.MarketComponent.LOWER_MARKET_LOSS_THRESHOLD;
import static gg.packetloss.grindstone.economic.store.sql.MarketDatabaseHelper.MARKET_INFO_COLUMNS;
import static gg.packetloss.grindstone.economic.store.sql.MarketDatabaseHelper.getMarketItem;
import static gg.packetloss.grindstone.util.DBUtil.preparePlaceHolders;
import static gg.packetloss.grindstone.util.DBUtil.setStringValues;

public class SQLItemStoreDatabase implements ItemStoreDatabase {
    private static final BigDecimal X_DIVISOR = BigDecimal.valueOf(50);

    private int getMaxAutoStock(BigDecimal baseValue) {
        baseValue = baseValue.add(X_DIVISOR).setScale(0, RoundingMode.HALF_UP);
        baseValue = baseValue.divide(X_DIVISOR, RoundingMode.HALF_UP);
        return (BigDecimal.valueOf(10000).divide(baseValue, RoundingMode.HALF_UP)).intValueExact();
    }

    private int getNewStock(BigDecimal baseValue, int existingStock) {
        int maxAutoStock = getMaxAutoStock(baseValue);
        int restockAmount = ChanceUtil.getRangedRandom(0, Math.max(1, maxAutoStock / 10));
        int restockedStock = Math.min(maxAutoStock, existingStock + restockAmount);

        return Math.max(existingStock, restockedStock);
    }

    private int applyStockNoise(BigDecimal baseValue, int newStock) {
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

    private int getNewStock(BigDecimal baseValue, int existingStock, int restockingRounds) {
        // If we have something in the lower market loss threshold, it's self managed.
        if (baseValue.compareTo(LOWER_MARKET_LOSS_THRESHOLD) >= 0) {
            return existingStock;
        }

        for (int i = 0; i < restockingRounds; ++i) {
            existingStock = getNewStock(baseValue, existingStock);
            existingStock = applyStockNoise(baseValue, existingStock);
        }

        return existingStock;
    }

    private BigDecimal getNewValue(BigDecimal baseValue, int newStock) {
        int targetStock = Math.max(10, getMaxAutoStock(baseValue));
        BigDecimal percentOutOfStock = BigDecimal.valueOf(Math.max(0d, targetStock - newStock) / targetStock);

        // Use multipliers that result in a net 0, from min - max fluctuation
        BigDecimal multiplier = BigDecimal.valueOf(.1);
        if (baseValue.compareTo(LOWER_MARKET_LOSS_THRESHOLD) >= 0) {
            multiplier = BigDecimal.valueOf(.04);
        }

        // Inject 30% noise
        double noise = ChanceUtil.getRangedRandom(.7d, 1d);
        multiplier = multiplier.multiply(BigDecimal.valueOf(noise));

        BigDecimal change = baseValue.multiply(multiplier);

        BigDecimal minPrice = baseValue.subtract(change);
        BigDecimal maxOverMin = BigDecimal.valueOf(2).multiply(change);
        return minPrice.add(percentOutOfStock.multiply(maxOverMin));
    }

    @Override
    public void updatePrices(int restockingRounds) {
        try (Connection connection = SQLHandle.getConnection()) {
            String updateSql = """
                UPDATE minecraft.market_items SET current_price = ?, stock = ? WHERE id = ?
            """;
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                String sql = """
                     SELECT id, price, stock FROM minecraft.market_items
                """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            int id = results.getInt(1);
                            BigDecimal price = results.getBigDecimal(2);
                            int stock = results.getInt(3);

                            int newStock = getNewStock(price, stock, restockingRounds);
                            updateStatement.setBigDecimal(1, getNewValue(price, newStock));
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
    public void scaleMarket(double factor) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                UPDATE minecraft.market_items SET price = price * ?, current_price = price * ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, factor);
                statement.setDouble(2, factor);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addItem(String itemName, BigDecimal price, boolean infinite, boolean disableBuy, boolean disableSell) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                INSERT INTO minecraft.market_items (name, price, current_price, infinite, buyable, sellable)
                    VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (name) DO UPDATE SET
                    price = excluded.price,
                    current_price = excluded.current_price,
                    infinite = excluded.infinite,
                    buyable = excluded.buyable,
                    sellable = excluded.sellable
            """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, itemName);
                statement.setBigDecimal(2, price);
                statement.setBigDecimal(3, price);
                statement.setBoolean(4, infinite);
                statement.setBoolean(5, !disableBuy);
                statement.setBoolean(6, !disableSell);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeItem(String itemName) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                DELETE FROM minecraft.market_items WHERE name = ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, itemName);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void adjustStocksCommon(List<MarketTransactionLine> transactionLines, boolean purchase) {
        try (Connection connection = SQLHandle.getConnection()) {
            String updateSql = """
                UPDATE minecraft.market_items SET stock = stock + ? WHERE name = ?
            """;
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                for (MarketTransactionLine transactionLine : transactionLines) {
                    if (transactionLine.getItem().hasInfiniteStock()) {
                        continue;
                    }

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
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT id FROM minecraft.market_items WHERE name = ?
            """;
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
        try (Connection connection  = SQLHandle.getConnection()) {
            String sql = """
                SELECT MARKET_INFO_COLUMNS FROM minecraft.market_items WHERE name = ?
            """.replace("MARKET_INFO_COLUMNS", MARKET_INFO_COLUMNS);
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

        try (Connection connection  = SQLHandle.getConnection()) {
            String sql = """
                SELECT MARKET_INFO_COLUMNS FROM minecraft.market_items WHERE name IN (MARKET_INFO_COLUMN_PLACEHOLDERS)
            """.replace(
                "MARKET_INFO_COLUMNS",
                MARKET_INFO_COLUMNS
            ).replace(
                "MARKET_INFO_COLUMN_PLACEHOLDERS",
                preparePlaceHolders(names.size())
            );

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
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT MARKET_INFO_COLUMNS FROM minecraft.market_items
            """.replace("MARKET_INFO_COLUMNS", MARKET_INFO_COLUMNS);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT MARKET_INFO_COLUMNS FROM minecraft.market_items WHERE split_part(name, ':', -1) LIKE ?
            """.replace("MARKET_INFO_COLUMNS", MARKET_INFO_COLUMNS);
            if (!showHidden) {
                sql += " AND (buyable = true OR sellable = true)";
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
