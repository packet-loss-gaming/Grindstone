/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite.db.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
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

public class MySQLPlayerInviteDatabase implements PlayerInviteDatabase {
    @Override
    public InviteResult addInvite(UUID existingPlayer, UUID newPlayer) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String checkSQL = "SELECT `invitee` FROM `player-invites` WHERE `player-invites`.`invitee` = ? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(checkSQL)) {
                statement.setString(1, newPlayer.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return InviteResult.ALREADY_INVITED;
                    }
                }
            }

            String insertSQL = "INSERT INTO `player-invites` (`invitor`, `invitee`) " +
                              "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?)";

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
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `l-p`.`UUID` FROM `player-invites` JOIN `lb-players` `l-p` ON " +
                         "`l-p`.playerid = `player-invites`.invitor WHERE `player-invites`.`invitee` = ? LIMIT 1";
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
        try (Connection connection = MySQLHandle.getConnection()) {
            String insertSQL = "UPDATE `player-invites` SET `player-invites`.`status` = ? " +
                "WHERE `player-invites`.`invitee` = ? AND `player-invites`.`status` = ?";

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
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `player-invites`.`id` FROM `player-invites` WHERE `player-invites`.`invitor` = " +
                "(SELECT `lb-players`.`playerid` FROM `lb-players` WHERE `lb-players`.`UUID` = ?) AND " +
                "`player-invites`.`status` = ?";
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

        try (Connection connection = MySQLHandle.getConnection()) {
            String insertSQL = "UPDATE `player-invites` SET `player-invites`.`status` = ? " +
                "WHERE `player-invites`.`id` IN (" + preparePlaceHolders(inviteIDs.size()) + ")";

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
