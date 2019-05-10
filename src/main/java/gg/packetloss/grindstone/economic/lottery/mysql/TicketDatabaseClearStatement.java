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

public class TicketDatabaseClearStatement implements MySQLPreparedStatement {

    private Connection con;

    @Override
    public void setConnection(Connection con) {
        this.con = con;
    }

    @Override
    public void executeStatements() throws SQLException {
        String sql = "TRUNCATE TABLE `lottery-tickets`";
        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
