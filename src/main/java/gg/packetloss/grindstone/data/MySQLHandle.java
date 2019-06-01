/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLHandle {
    private static String database = "";
    private static String username = "";
    private static String password = "";

    public static void setDatabase(String database) {
        MySQLHandle.database = database;
    }

    public static void setUsername(String username) {
        MySQLHandle.username = username;
    }

    public static void setPassword(String password) {
        MySQLHandle.password = password;
    }

    private static HikariDataSource pool = null;

    private static HikariDataSource getPool() {
        if (pool != null) {
            return pool;
        }

        pool = new HikariDataSource();
        pool.setJdbcUrl(database);
        pool.setUsername(username);
        pool.setPassword(password);

        pool.setMinimumIdle(4);
        pool.setMaximumPoolSize(8);
        pool.setPoolName("Grindstone-Connection-Pool");

        return pool;
    }

    public static Connection getConnection() throws SQLException {
        return getPool().getConnection();
    }

    public static int getPlayerId(String name) throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "SELECT playerid FROM `lb-players` WHERE playername = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                ResultSet results = statement.executeQuery();
                if (results.next()) return results.getInt(1);
            }
        }
        return -1;
    }

    public static String getPlayerName(int id) throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "SELECT playername FROM `lb-players` WHERE playerid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                ResultSet results = statement.executeQuery();
                if (results.next()) return results.getString(1);
            }
        }
        return null;
    }
}
