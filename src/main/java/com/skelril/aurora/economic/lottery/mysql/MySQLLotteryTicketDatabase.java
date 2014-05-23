/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery.mysql;

import com.skelril.aurora.data.MySQLHandle;
import com.skelril.aurora.data.MySQLPreparedStatement;
import com.skelril.aurora.economic.lottery.LotteryTicketDatabase;
import com.skelril.aurora.util.player.GenericWealthStore;
import com.skelril.aurora.util.player.WealthStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MySQLLotteryTicketDatabase implements LotteryTicketDatabase {

    private Queue<MySQLPreparedStatement> queue = new LinkedList<>();

    @Override
    public boolean load() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String mainSQL = "CREATE TABLE IF NOT EXISTS `lottery-tickets` (" +
                    "`id` INT NOT NULL AUTO_INCREMENT," +
                    "`player` INT NOT NULL," +
                    "`tickets` INT NOT NULL," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE INDEX `player` (`player`)" +
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
    public void addTickets(String playerName, int count) {
        try {
            queue.add(new TicketAdditionStatement(MySQLHandle.getPlayerId(playerName), count));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getTickets(String playerName) {
        try (Connection connection  = MySQLHandle.getConnection()) {
            String sql = "SELECT `tickets` FROM `lottery-tickets` WHERE `player` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, MySQLHandle.getPlayerId(playerName));
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return results.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void clearTickets() {
        queue.add(new TicketDatabaseClearStatement());
    }

    @Override
    public int getTicketCount() {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT SUM(tickets) from `lottery-tickets`";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) return results.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<GenericWealthStore> getTickets() {
        List<GenericWealthStore> tickets = new ArrayList<>();
        try (Connection connection = MySQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT `player`, `tickets` FROM `lottery-tickets`")) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        tickets.add(new WealthStore(MySQLHandle.getPlayerName(results.getInt(1)), results.getInt(2)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }
}
