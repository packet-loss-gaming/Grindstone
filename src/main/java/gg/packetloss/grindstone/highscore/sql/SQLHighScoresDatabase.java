/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.sql;

import gg.packetloss.grindstone.data.SQLHandle;
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

public class SQLHighScoresDatabase implements HighScoreDatabase {
    @Override
    public boolean deleteAllScores(ScoreType scoreType) {
        // For now limit this to goblet score types
        Validate.isTrue(scoreType instanceof GobletScoreType);

        try (Connection con = SQLHandle.getConnection()) {
            String sql = """
                DELETE FROM minecraft.high_scores WHERE score_type_id = ?
            """;
            try (PreparedStatement statement = con.prepareStatement(sql)) {
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
        // language=SQL
        String sql = """
            INSERT INTO minecraft.high_scores (player_id, score_type_id, value)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?, ?)
            ON CONFLICT (player_id, score_type_id) DO UPDATE SET value = high_scores.value + excluded.value
        """;
        batchUpdate(con, sql, updates);
    }

    private void overrideIfBetter(Connection con, ScoreType.Order order,
                                  List<HighScoreUpdate> updates) throws SQLException {
        // language=SQL
        String sql = """
            INSERT INTO minecraft.high_scores (player_id, score_type_id, value)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?, ?)
            ON CONFLICT (player_id, score_type_id) DO UPDATE SET value =
        """;
        if (order == ScoreType.Order.DESC) {
            sql += "greatest(";
        } else {
            sql += "least(";
        }
        sql += "high_scores.value, excluded.value)";
        batchUpdate(con, sql, updates);
    }

    private void overrideAlways(Connection con, List<HighScoreUpdate> updates) throws SQLException {
        // language=SQL
        String sql = """
            INSERT INTO minecraft.high_scores (player_id, score_type_id, value)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?, ?)
            ON CONFLICT (player_id, score_type_id) DO UPDATE SET value = excluded.value
        """;
        batchUpdate(con, sql, updates);
    }

    @Override
    public void batchProcess(List<HighScoreUpdate> scoresToUpdate) {
        Map<ScoreType, List<HighScoreUpdate>> groupedUpdates = groupUpdates(scoresToUpdate);

        try (Connection con = SQLHandle.getConnection()) {
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
        try (Connection con = SQLHandle.getConnection()) {
            String sql = """
                SELECT players.uuid, high_scores.value FROM minecraft.high_scores
                JOIN minecraft.players ON players.id = high_scores.player_id
                WHERE high_scores.score_type_id = ?
                ORDER BY high_scores.value
            """;
            if (scoreType.getOrder() == ScoreType.Order.ASC) {
                sql += " ASC";
            } else {
                sql += " DESC";
            }
            sql += " LIMIT ?";

            try (PreparedStatement statement = con.prepareStatement(sql)) {
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
        try (Connection con = SQLHandle.getConnection()) {
            String isEmptySql = """
                SELECT id FROM minecraft.high_scores WHERE score_type_id = ? LIMIT 1
            """;
            try (PreparedStatement statement = con.prepareStatement(isEmptySql)) {
                statement.setInt(1, scoreType.getId());

                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        return Optional.empty();
                    }
                }
            }

            String sql = """
                SELECT AVG(high_scores.value) FROM minecraft.high_scores WHERE score_type_id = ?
            """;
            try (PreparedStatement statement = con.prepareStatement(sql)) {
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
