/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.highscore.HighScoreDatabase;
import gg.packetloss.grindstone.highscore.HighScoreUpdate;
import gg.packetloss.grindstone.highscore.ScoreEntry;
import gg.packetloss.grindstone.highscore.scoretype.GobletScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import org.apache.commons.lang.Validate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLHighScoresDatabase implements HighScoreDatabase {
    @Override
    public boolean deleteAllScores(ScoreType scoreType) {
        // For now limit this to goblet score types
        Validate.isTrue(scoreType instanceof GobletScoreType);

        try (Connection con = MySQLHandle.getConnection()) {
            String SQL = "DELETE FROM `high-scores` WHERE `score-type-id` = ?";

            try (PreparedStatement statement = con.prepareStatement(SQL)) {
                statement.setInt(1, scoreType.getId());
                statement.execute();

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Map<ScoreType, List<HighScoreUpdate>> groupUpdates(List<HighScoreUpdate> scoresToUpdate) {
        return scoresToUpdate.stream().collect(Collectors.groupingBy(HighScoreUpdate::getScoreType));
    }

    private void batchUpdate(Connection con, String SQL, List<HighScoreUpdate> updates) throws SQLException {
        try (PreparedStatement statement = con.prepareStatement(SQL)) {
            for (HighScoreUpdate update : updates) {
                statement.setString(1, update.getPlayerId().toString());
                statement.setInt(2, update.getScoreType().getId());
                statement.setBigDecimal(3, new BigDecimal(update.getValue()));

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private void incrementalUpdate(Connection con, List<HighScoreUpdate> updates) throws SQLException {
        String SQL = "INSERT INTO `high-scores` (`player-id`, `score-type-id`, `value`) " +
                "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?) " +
                "ON DUPLICATE KEY UPDATE value = values(value) + value";
        batchUpdate(con, SQL, updates);
    }

    private void overrideIfBetter(Connection con, ScoreType.Order order,
                                  List<HighScoreUpdate> updates) throws SQLException {
        String SQL = "INSERT INTO `high-scores` (`player-id`, `score-type-id`, `value`) " +
                "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?) " +
                "ON DUPLICATE KEY UPDATE value = " + (order == ScoreType.Order.DESC ? "greatest(" : "least(") + "value, values(value))";
        batchUpdate(con, SQL, updates);
    }

    private void overrideAlways(Connection con, List<HighScoreUpdate> updates) throws SQLException {
        String SQL = "INSERT INTO `high-scores` (`player-id`, `score-type-id`, `value`) " +
            "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?) " +
            "ON DUPLICATE KEY UPDATE value = values(value)";
        batchUpdate(con, SQL, updates);
    }

    @Override
    public void batchProcess(List<HighScoreUpdate> scoresToUpdate) {
        Map<ScoreType, List<HighScoreUpdate>> groupedUpdates = groupUpdates(scoresToUpdate);

        try (Connection con = MySQLHandle.getConnection()) {
            for (Map.Entry<ScoreType, List<HighScoreUpdate>> updateSet : groupedUpdates.entrySet()) {
                ScoreType scoreType = updateSet.getKey();
                List<HighScoreUpdate> updatesForScoreType = updateSet.getValue();

                switch (scoreType.getUpdateMethod()) {
                    case INCREMENTAL -> {
                        incrementalUpdate(con, updatesForScoreType);
                    }
                    case OVERRIDE_IF_BETTER -> {
                        overrideIfBetter(con, scoreType.getOrder(), updatesForScoreType);
                    }
                    case OVERRIDE_ALWAYS -> {
                        overrideAlways(con, updatesForScoreType);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<List<ScoreEntry>> getTop(ScoreType scoreType, int count) {
        try (Connection con = MySQLHandle.getConnection()) {
            String SQL = "SELECT `lb-players`.`uuid`, `high-scores`.`value` FROM `high-scores` " +
                    "JOIN `lb-players` ON `high-scores`.`player-id` = `lb-players`.`playerid` " +
                    "WHERE `high-scores`.`score-type-id` = ? " +
                    "ORDER BY `high-scores`.`value`" + (scoreType.getOrder() == ScoreType.Order.ASC ? "ASC" : "DESC") +
                    " LIMIT ?";

            try (PreparedStatement statement = con.prepareStatement(SQL)) {
                statement.setInt(1, scoreType.getId());
                statement.setInt(2, count);

                try (ResultSet results = statement.executeQuery()) {
                    List<ScoreEntry> topScores = new ArrayList<>();

                    while (results.next()) {
                        topScores.add(new ScoreEntry(
                                UUID.fromString(results.getString(1)),
                                results.getInt(2)
                        ));
                    }

                    return Optional.of(topScores);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getAverageScore(ScoreType scoreType) {
        try (Connection con = MySQLHandle.getConnection()) {
            String IS_EMPTY_SQL = "SELECT `high-scores`.`id` FROM `high-scores` " +
                    "WHERE `high-scores`.`score-type-id` = ? LIMIT 1";

            try (PreparedStatement statement = con.prepareStatement(IS_EMPTY_SQL)) {
                statement.setInt(1, scoreType.getId());

                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        return Optional.empty();
                    }
                }
            }

            String SQL = "SELECT AVG(`high-scores`.`value`) FROM `high-scores` " +
                    "WHERE `high-scores`.`score-type-id` = ?";

            try (PreparedStatement statement = con.prepareStatement(SQL)) {
                statement.setInt(1, scoreType.getId());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(results.getInt(1));
                    }

                    throw new IllegalStateException();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
