/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory.db;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.world.type.city.area.areas.Factory.FactoryJob;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gg.packetloss.grindstone.util.DBUtil.preparePlaceHolders;
import static gg.packetloss.grindstone.util.DBUtil.setStringValues;

public class MySQLFactoryJobDatabase implements FactoryJobDatabase {
    @Override
    public Optional<FactoryJob> getJob(UUID playerID, String itemName) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String searchSQL = "SELECT `amount` FROM `factory-jobs` WHERE `player-id` = " +
                "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1) AND `produced-item` = ?";

            try (PreparedStatement statement = connection.prepareStatement(searchSQL)) {
                statement.setString(1, playerID.toString());
                statement.setString(2, itemName);

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(new FactoryJob(playerID, itemName, results.getInt(1)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private void cleanupStaleJobs(Connection connection, Stream<FactoryJob> factoryJobs) throws SQLException {
        String SQL = "DELETE FROM `factory-jobs` WHERE `player-id` = " +
            "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1) AND `produced-item` = ?";

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            factoryJobs.forEach((factoryJob -> {
                try {
                    statement.setString(1, factoryJob.getPlayerID().toString());
                    statement.setString(2, factoryJob.getItemName());
                    statement.addBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));

            statement.executeBatch();
        }
    }

    private void insertOrUpdateActiveJobs(Connection connection, Stream<FactoryJob> factoryJobs) throws SQLException {
        String SQL = "INSERT INTO `factory-jobs` (`player-id`, `produced-item`, `amount`) " +
            "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?) " +
            "ON DUPLICATE KEY UPDATE `amount` = values(amount)";

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            factoryJobs.forEach((factoryJob -> {
                try {
                    statement.setString(1, factoryJob.getPlayerID().toString());
                    statement.setString(2, factoryJob.getItemName());
                    statement.setInt(3, factoryJob.getItemsRemaining());
                    statement.addBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));

            statement.executeBatch();
        }
    }

    @Override
    public void updateJobs(List<FactoryJob> factoryJobs) {
        try (Connection connection = MySQLHandle.getConnection()) {
            connection.setAutoCommit(false);

            cleanupStaleJobs(connection, factoryJobs.stream().filter(FactoryJob::isComplete));
            insertOrUpdateActiveJobs(connection, factoryJobs.stream().filter(FactoryJob::isIncomplete));

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<FactoryJob> getJobs(List<UUID> activePlayers) {
        if (activePlayers.isEmpty()) {
            return new ArrayList<>();
        }

        try (Connection connection = MySQLHandle.getConnection()) {
            String searchSQL = "SELECT `lb-players`.`uuid`, `produced-item`, `amount` FROM `factory-jobs` " +
                "JOIN `lb-players` ON `factory-jobs`.`player-id` = `lb-players`.`playerid` " +
                "WHERE `lb-players`.`uuid` IN (" + preparePlaceHolders(activePlayers.size()) + ")";

            try (PreparedStatement statement = connection.prepareStatement(searchSQL)) {
                setStringValues(statement, activePlayers.stream().map(UUID::toString).collect(Collectors.toList()));

                try (ResultSet results = statement.executeQuery()) {
                    List<FactoryJob> factoryJobs = new ArrayList<>();
                    while (results.next()) {
                        factoryJobs.add(new FactoryJob(
                            UUID.fromString(results.getString(1)),
                            results.getString(2),
                            results.getInt(3)
                        ));
                    }
                    return factoryJobs;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
