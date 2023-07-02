/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet.database.sql;

import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.economic.wallet.database.WalletDatabase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SQLWalletDatabase implements WalletDatabase {

    private BigDecimal getBalance(Connection connection, UUID playerID) throws SQLException {
        String SQL = """
            SELECT balance from minecraft.player_wallets WHERE player_id =
              (SELECT id FROM minecraft.players AS players WHERE uuid = ? LIMIT 1)
        """;
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private void adjustBalance(Connection connection, UUID playerID, BigDecimal amount) throws SQLException {
        String SQL = """
            INSERT INTO minecraft.player_wallets (player_id, balance)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?)
            ON CONFLICT (player_id) DO UPDATE SET balance = player_wallets.balance + excluded.balance
        """;
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());
            statement.setBigDecimal(2, amount);

            statement.execute();
        }
    }

    @Override
    public BigDecimal getBalance(UUID playerID) {
        try (Connection connection = SQLHandle.getConnection()) {
            return getBalance(connection, playerID);
        } catch (SQLException t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public BigDecimal addToBalance(UUID playerID, BigDecimal amount) {
        try (Connection connection = SQLHandle.getConnection()) {
            adjustBalance(connection, playerID, amount);
            return getBalance(connection, playerID);
        } catch (SQLException t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean removeFromBalance(UUID playerID, BigDecimal amount) {
        try (Connection connection = SQLHandle.getConnection()) {
            connection.setAutoCommit(false);

            adjustBalance(connection, playerID, amount.negate());

            BigDecimal balance = getBalance(connection, playerID);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                connection.rollback();
                return true;
            }

            connection.commit();
            return false;
        } catch (SQLException t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void setBalance(UUID playerID, BigDecimal amount) {
        String SQL = """
            INSERT INTO minecraft.player_wallets (player_id, balance)
            VALUES ((SELECT id FROM minecraft.players WHERE uuid = ? LIMIT 1), ?)
            ON CONFLICT (player_id) DO UPDATE SET balance = excluded.balance
        """;
        try (Connection connection = SQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(SQL)) {
                statement.setString(1, playerID.toString());
                statement.setBigDecimal(2, amount);

                statement.execute();
            }
        } catch (SQLException t) {
            throw new RuntimeException(t);
        }
    }
}
