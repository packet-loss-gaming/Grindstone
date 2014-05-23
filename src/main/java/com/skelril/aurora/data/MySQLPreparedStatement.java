/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.data;

import java.sql.Connection;
import java.sql.SQLException;

public interface MySQLPreparedStatement {
    void setConnection(Connection connection);
    void executeStatements() throws SQLException;
}
