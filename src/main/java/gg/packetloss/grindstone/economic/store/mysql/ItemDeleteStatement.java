/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemDeleteStatement extends ItemStatement {

  private Connection con;

  public ItemDeleteStatement(String name) {
    super(name);
  }

  @Override
  public void setConnection(Connection con) {
    this.con = con;
  }

  @Override
  public void executeStatements() throws SQLException {
    String sql = "DELETE FROM `market-items` WHERE `name` = ?";
    try (PreparedStatement statement = con.prepareStatement(sql)) {
      statement.setString(1, name);
      statement.execute();
    }
  }
}
