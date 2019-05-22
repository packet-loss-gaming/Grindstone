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

public class MySQLItemStoreDatabase implements ItemStoreDatabase {
  private static final String columns = "`name`, `price`, `current-price`, `stock`, `buyable`, `sellable`";
  private Queue<MySQLPreparedStatement> queue = new LinkedList<>();

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
  public boolean load() {
    try (Connection connection = MySQLHandle.getConnection()) {
      String mainSQL = "CREATE TABLE IF NOT EXISTS `market-items` (" +
          "`id` INT NOT NULL AUTO_INCREMENT," +
          "`name` VARCHAR(50) NOT NULL," +
          "`price` DOUBLE NOT NULL," +
          "`current-price` DOUBLE NOT NULL," +
          "`stock` INT NOT NULL," +
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
    if (queue.isEmpty()) {
      return true;
    }
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

  private double getNewValue(double baseValue) {
    double divisor = 5 + ChanceUtil.getRandom(5);
    if (baseValue >= LOWER_MARKET_LOSS_THRESHOLD) {
      divisor += 7;
    }

    double multiplier = 1D / divisor;
    double change = baseValue * multiplier;

    if (ChanceUtil.getChance(2)) {
      change = -change;
    }

    return baseValue + change;
  }

  private int getNewStock(double baseValue, int existingStock) {
    final int TARGET_MAXIMUM = 1000000;
    final int OFFSET = 100;

    int adjustedChange = (int) ChanceUtil.getRandom(TARGET_MAXIMUM / (baseValue + OFFSET));

    if (ChanceUtil.getChance(2)) {
      existingStock += adjustedChange;
    } else {
      existingStock -= adjustedChange * ChanceUtil.getRandomNTimes(15, 4);
    }

    return Math.max(0, existingStock);
  }

  @Override
  public void updatePrices() {
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

              updateStatement.setDouble(1, getNewValue(price));
              updateStatement.setInt(2, getNewStock(price, stock));
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

  @Override
  public MarketItemInfo getItem(String name) {
    try (Connection connection = MySQLHandle.getConnection()) {
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
  public Map<String, MarketItemInfo> getItems(Collection<String> names) {
    Map<String, MarketItemInfo> nameItemMapping = new HashMap<>();

    try (Connection connection = MySQLHandle.getConnection()) {
      String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` IN (" + preparePlaceHolders(names.size()) + ")";
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        List<String> uppercaseNames = names.stream().map(String::toUpperCase).collect(Collectors.toList());
        setValues(statement, uppercaseNames);

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
      String sql = "SELECT " + columns + " FROM `market-items` WHERE `name` LIKE ?";
      if (!showHidden) {
        sql += " AND (`buyable` = true OR `sellable` = true)";
      }
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, filter.toUpperCase() + "%");
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
