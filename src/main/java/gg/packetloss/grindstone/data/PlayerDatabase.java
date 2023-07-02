/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class PlayerDatabase {
    public void recordPlayerLogin(UUID playerID, String playerName, String ipAddress) throws SQLException{
        String sql = """
            INSERT INTO minecraft.players (uuid, name, first_login, last_login, ip)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (uuid) DO UPDATE SET
                name = excluded.name,
                last_login = excluded.last_login,
                ip = excluded.ip
        """;
        try (Connection connection = SQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                Timestamp loginTime = Timestamp.from(Instant.now());
                statement.setString(1, playerID.toString());
                statement.setString(2, playerName);
                statement.setTimestamp(3, loginTime);
                statement.setTimestamp(4, loginTime);
                statement.setString(5, ipAddress);
                statement.execute();
            }
        }
    }

    public void recordPlayerLogout(UUID playerID, long onlineSeconds) {
        try (Connection connection = SQLHandle.getConnection()) {
            String updateSql = """
                UPDATE minecraft.players SET online_time = online_time + ? WHERE uuid = ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setLong(1, onlineSeconds);
                statement.setString(2, playerID.toString());
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
