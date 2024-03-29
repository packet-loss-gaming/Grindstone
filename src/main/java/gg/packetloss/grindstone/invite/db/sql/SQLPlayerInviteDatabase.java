/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite.db.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.invite.db.InviteResult;
import gg.packetloss.grindstone.invite.db.InviteStatus;
import gg.packetloss.grindstone.invite.db.PlayerInviteDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gg.packetloss.grindstone.util.DBUtil.preparePlaceHolders;

public class SQLPlayerInviteDatabase implements PlayerInviteDatabase {
    @Override
    public InviteResult addInvite(UUID existingPlayer, UUID newPlayer) {
        try (Connection connection = SQLHandle.getConnection()) {
            String checkSQL = """
                SELECT invitee FROM minecraft.player_invites WHERE invitee = ? LIMIT 1
            """;
            try (PreparedStatement statement = connection.prepareStatement(checkSQL)) {
                statement.setString(1, newPlayer.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return InviteResult.ALREADY_INVITED;
                    }
                }
            }

            String insertSQL = """
                INSERT INTO minecraft.player_invites (invitor, invitee)
                VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?)
            """;
            try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
                statement.setString(1, existingPlayer.toString());
                statement.setString(2, newPlayer.toString());

                statement.execute();
            }
            return InviteResult.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<UUID> getInvitor(UUID newPlayer) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT players.uuid FROM minecraft.player_invites
                JOIN minecraft.players ON players.id = player_invites.invitor
                WHERE invitee = ? LIMIT 1
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newPlayer.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        String invitor = results.getString(1);

                        return Optional.of(UUID.fromString(invitor));
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markNewPlayerJoined(UUID newPlayer) {
        try (Connection connection = SQLHandle.getConnection()) {
            String insertSQL = """
                UPDATE minecraft.player_invites SET status = ? WHERE invitee = ? AND status = ?
            """;

            try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
                statement.setInt(1, InviteStatus.JOINED.ordinal());
                statement.setString(2, newPlayer.toString());
                statement.setInt(3, InviteStatus.INVITED.ordinal());

                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Integer> getInvitesByStatus(UUID existingPlayer, InviteStatus status) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT id FROM minecraft.player_invites
                    WHERE
                        invitor = (SELECT id FROM minecraft.players WHERE uuid = ?)
                    AND
                        status = ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, existingPlayer.toString());
                statement.setInt(2, status.ordinal());

                try (ResultSet results = statement.executeQuery()) {
                    List<Integer> playerIDs = new ArrayList<>();

                    while (results.next()) {
                        playerIDs.add(results.getInt(1));
                    }
                    return playerIDs;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setInviteStatus(List<Integer> inviteIDs, InviteStatus newStatus) {
        if (inviteIDs.isEmpty()) {
            return;
        }

        try (Connection connection = SQLHandle.getConnection()) {
            String insertSQL = """
                UPDATE minecraft.player_invites SET status = ?
                WHERE id IN (
            """;
            insertSQL += preparePlaceHolders(inviteIDs.size()) + ')';

            try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
                statement.setInt(1, newStatus.ordinal());
                int idx = 0;
                for (Integer inviteID : inviteIDs) {
                    statement.setInt(2 + idx, inviteID);
                    ++idx;
                }

                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }}
}
