/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite.db.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.invite.db.InviteResult;
import gg.packetloss.grindstone.invite.db.PlayerInviteDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

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
            String sql = "select `l-p`.`UUID` from `player-invites` join `lb-players` `l-p` on " +
                         "`l-p`.playerid = `player-invites`.invitor where `player-invites`.`invitee` = ? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newPlayer.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        String invitor = results.getString(1);

                        return Optional.of(UUID.fromString(invitor));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
