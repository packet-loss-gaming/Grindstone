/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.economic.lottery.LotteryTicketDatabase;
import gg.packetloss.grindstone.economic.lottery.LotteryTicketEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLLotteryTicketDatabase implements LotteryTicketDatabase {
    @Override
    public void addTickets(UUID playerID, int count) {
        String sql = """
            INSERT INTO minecraft.lottery_tickets (player_id, tickets)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?)
            ON CONFLICT (player_id) DO UPDATE SET tickets = lottery_tickets.tickets + excluded.tickets
        """;
        try (Connection connection = SQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());
                statement.setInt(2, count);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCPUTickets(int count) {
        addTickets(SQLHandle.CPU_ID, count);
    }

    @Override
    public int getTickets(UUID playerID) {
        try (Connection connection  = SQLHandle.getConnection()) {
            String sql = """
                SELECT tickets FROM minecraft.lottery_tickets WHERE player_id =
                    (SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1)
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return results.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void clearTickets() {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                TRUNCATE TABLE minecraft.lottery_tickets
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getTicketCount() {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT SUM(tickets) FROM minecraft.lottery_tickets
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return results.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<LotteryTicketEntry> getTickets() {
        List<LotteryTicketEntry> tickets = new ArrayList<>();
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT players.uuid, lottery.tickets FROM minecraft.lottery_tickets AS lottery
                JOIN minecraft.players AS players ON players.id = lottery.player_id
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        UUID playerID = UUID.fromString(results.getString(1));
                        int ticketCount = results.getInt(2);
                        tickets.add(new LotteryTicketEntry(playerID, ticketCount));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }
}
