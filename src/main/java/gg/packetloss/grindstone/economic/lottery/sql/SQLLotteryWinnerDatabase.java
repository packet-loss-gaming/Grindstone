/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.economic.lottery.LotteryWinner;
import gg.packetloss.grindstone.economic.lottery.LotteryWinnerDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLLotteryWinnerDatabase implements LotteryWinnerDatabase {
    @Override
    public void addWinner(UUID playerID, BigDecimal amount) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                INSERT INTO minecraft.lottery_winners (date, player_id, amount)
                VALUES (?, (SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?)
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.from(Instant.now()));
                statement.setString(2, playerID.toString());
                statement.setBigDecimal(3, amount);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCPUWin(BigDecimal amount) {
        addWinner(SQLHandle.CPU_ID, amount);
    }

    @Override
    public List<LotteryWinner> getRecentWinner(int limit) {
        List<LotteryWinner> winners = new ArrayList<>();
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT players.uuid, lottery.amount FROM minecraft.lottery_winners AS lottery
                JOIN minecraft.players AS players ON players.id = lottery.player_id
                ORDER BY lottery.date DESC LIMIT ?;
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        UUID playerID = UUID.fromString(results.getString(1));
                        BigDecimal amount = results.getBigDecimal(2);
                        winners.add(new LotteryWinner(playerID, amount));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winners;
    }
}
