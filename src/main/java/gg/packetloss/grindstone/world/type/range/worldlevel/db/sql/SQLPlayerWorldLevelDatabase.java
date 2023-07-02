/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.db.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.world.type.range.worldlevel.db.PlayerWorldLevelDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class SQLPlayerWorldLevelDatabase implements PlayerWorldLevelDatabase {
    @Override
    public Optional<Integer> loadWorldLevel(UUID playerID) {
        try (Connection connection = SQLHandle.getConnection()) {
            String SQL = """
                SELECT level FROM minecraft.player_world_levels WHERE player_id =
                (SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1)
            """;
            try (PreparedStatement statement = connection.prepareStatement(SQL)) {
                statement.setString(1, playerID.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(results.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private void updateWorldLevel(Connection connection, UUID playerID, int newLevel) throws SQLException {
        String SQL = """
            INSERT INTO minecraft.player_world_levels (player_id, level)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?)
            ON CONFLICT (player_id) DO UPDATE SET level = excluded.level
        """;
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());
            statement.setInt(2, newLevel);

            statement.execute();
        }
    }

    private void resetWorldLevel(Connection connection, UUID playerID) throws SQLException {
        String SQL = """
            DELETE FROM minecraft.player_world_levels WHERE player_id =
            (SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1)
        """;
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());
            statement.execute();
        }
    }

    @Override
    public void updateWorldLevel(UUID playerID, int newLevel) {
        try (Connection connection = SQLHandle.getConnection()) {
            if (newLevel > 1) {
                updateWorldLevel(connection, playerID, newLevel);
            } else {
                resetWorldLevel(connection, playerID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
