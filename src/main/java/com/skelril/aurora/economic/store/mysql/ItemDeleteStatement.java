/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.store.mysql;

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
