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
