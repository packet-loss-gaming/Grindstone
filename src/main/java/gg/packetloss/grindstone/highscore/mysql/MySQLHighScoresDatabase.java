package gg.packetloss.grindstone.highscore.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.highscore.HighScoreDatabase;
import gg.packetloss.grindstone.highscore.HighScoreUpdate;
import gg.packetloss.grindstone.highscore.ScoreEntry;
import gg.packetloss.grindstone.highscore.ScoreType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLHighScoresDatabase implements HighScoreDatabase {
//    private Optional<Integer> get(Connection con, UUID playerId, ScoreType scoreType) throws SQLException {
//        String SQL = "SELECT `high-scores`.`value` FROM `high-scores` " +
//                "WHERE `high-scores`.`player-id` IN " +
//                "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ?) " +
//                "AND `high-scores`.`score-type-id` = ?";
//
//        try (PreparedStatement statement = con.prepareStatement(SQL)) {
//            statement.setString(1, playerId.toString());
//            statement.setInt(2, scoreType.getId());
//
//            try (ResultSet results = statement.executeQuery()) {
//                if (results.next()) {
//                    return Optional.of(results.getInt(1));
//                }
//
//                return Optional.empty();
//            }
//        }
//    }

    private void incrementalUpdate(Connection con, UUID playerId, ScoreType scoreType, int amt) throws SQLException {
        String SQL = "INSERT INTO `high-scores` (`player-id`, `score-type-id`, `value`) " +
                "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?) " +
                "ON DUPLICATE KEY UPDATE value = values(value) + value";

        try (PreparedStatement statement = con.prepareStatement(SQL)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, scoreType.getId());
            statement.setInt(3, amt);

            statement.execute();
        }
    }

    private void overrideIfBetter(Connection con, UUID playerId, ScoreType scoreType, int value) throws SQLException {
        String SQL = "INSERT INTO `high-scores` (`player-id`, `score-type-id`, `value`) " +
                "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?) " +
                "ON DUPLICATE KEY UPDATE value = " + (scoreType.getOrder() == ScoreType.Order.DESC ? "greatest(" : "least(") + "value, values(value))";

        try (PreparedStatement statement = con.prepareStatement(SQL)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, scoreType.getId());
            statement.setInt(3, value);

            statement.execute();
        }
    }

//    private void overrideIfBetter(UUID playerId, ScoreType scoreType, int value) {
//        Optional<Integer> optExistingValue = get(playerId, scoreType);
//        if (!optExistingValue.isPresent()) {
//            override(playerId, scoreType, value);
//            return;
//        }
//
//        int existingValue = optExistingValue.get();
//        if (scoreType.getOrder() == ScoreType.Order.DESC) {
//            if (value <= existingValue) {
//                return;
//            }
//        } else {
//            if (existingValue <= value) {
//                return;
//            }
//        }
//
//        override(playerId, scoreType, value);
//    }

    @Override
    public void batchProcess(List<HighScoreUpdate> scoresToUpdate) {
        try (Connection con = MySQLHandle.getConnection()) {
            for (HighScoreUpdate update : scoresToUpdate) {
                if (update.getScoreType().isIncremental()) {
                    incrementalUpdate(con, update.getPlayerId(), update.getScoreType(), update.getValue());
                } else {
                    overrideIfBetter(con, update.getPlayerId(), update.getScoreType(), update.getValue());
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
