/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class SQLHandle {
    public static final UUID CPU_ID = UUID.fromString("c0321170-74eb-4f24-b559-b3cb8dc1ddc1");

    private static String database = "";
    private static String username = "";
    private static String password = "";

    public static void setDatabase(String database) {
        SQLHandle.database = database;
    }

    public static void setUsername(String username) {
        SQLHandle.username = username;
    }

    public static void setPassword(String password) {
        SQLHandle.password = password;
    }

    private static HikariDataSource pool = null;
    private static ReentrantLock setupLock = new ReentrantLock();

    private static DataSource getDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(database);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static HikariDataSource createNewPool() {
        HikariDataSource newPool = new HikariDataSource();
        newPool.setDataSource(getDataSource());
        newPool.setMinimumIdle(4);
        newPool.setMaximumPoolSize(8);
        newPool.setPoolName("Grindstone-Connection-Pool");
        return newPool;
    }

    private static void setupPool() {
        setupLock.lock();

        try {
            // Some other thread did this for us already.
            if (pool != null) {
                return;
            }

            // We specifically want to set the pool, after it's been setup,
            // so that the pointer is only ever set to a complete pool.
            pool = createNewPool();
        } finally {
            setupLock.unlock();
        }
    }

    private static HikariDataSource getPool() {
        // No lock is required for this check as this is an atomic action.
        if (pool == null) {
            setupPool();
        }

        return pool;
    }

    public static Connection getConnection() throws SQLException {
        return getPool().getConnection();
    }

    public static Optional<Integer> getPlayerInternalID(UUID playerID) throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = """
                SELECT id FROM minecraft.players WHERE uuid = ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());
                ResultSet results = statement.executeQuery();
                if (results.next()) return Optional.of(results.getInt(1));
            }
        }
        return Optional.empty();
    }

    public static Optional<UUID> getPlayerUUID(int id) throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = """
                SELECT uuid FROM minecraft.players WHERE id = ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                ResultSet results = statement.executeQuery();
                if (results.next()) {
                    return Optional.of(UUID.fromString(results.getString(1)));
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Long> getOnlineTime(UUID playerID) throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = """
                SELECT online_time FROM minecraft.players WHERE UUID = ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());
                ResultSet results = statement.executeQuery();
                if (results.next()) return Optional.of(results.getLong(1));
            }
        }
        return Optional.empty();
    }
}
