/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery.mysql;

import gg.packetloss.grindstone.data.MySQLPreparedStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LotteryWinnerStatement implements MySQLPreparedStatement {

  private final long date;
  private final int player;
  private final double amount;
  private Connection con;

  public LotteryWinnerStatement(int player, double amount) {
    this.date = System.currentTimeMillis() / 1000;
    this.player = player;
    this.amount = amount;
  }

  @Override
  public void setConnection(Connection con) {
    this.con = con;
  }

  @Override
  public void executeStatements() throws SQLException {
    String sql = "INSERT INTO `lottery-winners` (date, player, amount) VALUES (FROM_UNIXTIME(?), ?, ?)";

    try (PreparedStatement statement = con.prepareStatement(sql)) {
      statement.setLong(1, date);
      statement.setInt(2, player);
      statement.setDouble(3, amount);
      statement.execute();
    }
  }
}
