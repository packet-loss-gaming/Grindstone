/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.data.MySQLPreparedStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemTransactionStatement implements MySQLPreparedStatement {

  private final long date;
  private final int playerID;
  private final int itemID;
  private final int quantity;
  private Connection con;

  public ItemTransactionStatement(int playerID, int itemID, int quantity) {
    this.date = System.currentTimeMillis() / 1000;
    this.playerID = playerID;
    this.itemID = itemID;
    this.quantity = quantity;
  }

  @Override
  public void setConnection(Connection con) {
    this.con = con;
  }

  @Override
  public void executeStatements() throws SQLException {
    String sql = "INSERT INTO `market-transactions` (date, player, item, amount) VALUES (FROM_UNIXTIME(?), ?, ?, ?)";

    try (PreparedStatement statement = con.prepareStatement(sql)) {
      statement.setLong(1, date);
      statement.setInt(2, playerID);
      statement.setInt(3, itemID);
      statement.setInt(4, quantity);
      statement.execute();
    }
  }
}
