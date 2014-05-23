/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.store.mysql;

import com.skelril.aurora.data.MySQLPreparedStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemTransactionStatement implements MySQLPreparedStatement {

    private Connection con;

    private final long date;
    private final int playerID;
    private final int itemID;
    private final int quantity;

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
