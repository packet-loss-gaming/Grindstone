/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.data.MySQLPreparedStatement;
import gg.packetloss.grindstone.economic.lottery.LotteryWinner;
import gg.packetloss.grindstone.economic.lottery.LotteryWinnerDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static gg.packetloss.grindstone.economic.lottery.LotteryTicketDatabase.CPU_ID;

public class MySQLLotteryWinnerDatabase implements LotteryWinnerDatabase {

    private Queue<MySQLPreparedStatement> queue = new LinkedList<>();

    @Override
    public boolean load() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String mainSQL = "CREATE TABLE IF NOT EXISTS `lottery-winners` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`date` DATETIME NOT NULL," +
                    "`player` INT NOT NULL," +
                    "`amount` DOUBLE NOT NULL," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=MyISAM;";
            try (PreparedStatement statement = connection.prepareStatement(mainSQL)) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    public void addWinner(UUID playerID, double amount) {
        try {
            queue.add(new LotteryWinnerStatement(MySQLHandle.getPlayerInternalID(playerID).get(), amount));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCPUWin(double amount) {
        queue.add(new LotteryWinnerStatement(-1, amount));
    }

    @Override
    public List<LotteryWinner> getRecentWinner(int limit) {
        List<LotteryWinner> winners = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `player`, `amount` FROM `lottery-winners` ORDER BY `date` DESC LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        int internalID = results.getInt(1);
                        UUID playerID = internalID == -1 ? CPU_ID : MySQLHandle.getPlayerUUID(internalID).get();
                        winners.add(new LotteryWinner(playerID, results.getDouble(2)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winners;
    }
}
