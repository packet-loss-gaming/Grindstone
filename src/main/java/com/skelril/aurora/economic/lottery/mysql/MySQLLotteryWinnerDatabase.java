/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery.mysql;

import com.skelril.aurora.data.MySQLHandle;
import com.skelril.aurora.data.MySQLPreparedStatement;
import com.skelril.aurora.economic.lottery.LotteryWinner;
import com.skelril.aurora.economic.lottery.LotteryWinnerDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
    public void addWinner(String name, double amount) {
        try {
            queue.add(new LotteryWinnerStatement(MySQLHandle.getPlayerId(name), amount));
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                        winners.add(new LotteryWinner(MySQLHandle.getPlayerName(results.getInt(1)), results.getDouble(2)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winners;
    }
}
