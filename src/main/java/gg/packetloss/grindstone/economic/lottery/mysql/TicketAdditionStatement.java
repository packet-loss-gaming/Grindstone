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

public class TicketAdditionStatement implements MySQLPreparedStatement {

    private Connection con;
    private final int player;
    private final int amount;

    public TicketAdditionStatement(int player, int amount) {
        this.player = player;
        this.amount = amount;
    }

    @Override
    public void setConnection(Connection con) {
        this.con = con;
    }

    @Override
    public void executeStatements() throws SQLException {
        String sql = "INSERT INTO `lottery-tickets` (player, tickets) VALUES (?, ?)"
                + "ON DUPLICATE KEY UPDATE tickets=values(tickets) + tickets";
        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, player);
            statement.setInt(2, amount);
            statement.execute();
        }
    }
}
