/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery.mysql;

import com.skelril.aurora.data.MySQLPreparedStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LotteryWinnerStatement implements MySQLPreparedStatement {

    private Connection con;
    private final long date;
    private final int player;
    private final double amount;

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
