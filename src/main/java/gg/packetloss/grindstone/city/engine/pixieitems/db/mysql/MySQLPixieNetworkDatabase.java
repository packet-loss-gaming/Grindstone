package gg.packetloss.grindstone.city.engine.pixieitems.db.mysql;

import gg.packetloss.grindstone.city.engine.pixieitems.db.PixieNetworkDatabase;
import gg.packetloss.grindstone.data.MySQLHandle;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class MySQLPixieNetworkDatabase implements PixieNetworkDatabase {
    @Override
    public Optional<Integer> createNetwork(UUID namespace, String name) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "INSERT INTO `pixie-networks` (namespace, name) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, namespace.toString());
                statement.setString(2, name);
                statement.execute();

                try (ResultSet results = statement.getGeneratedKeys()) {
                    if (results.next()) {
                        return Optional.of(results.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<Integer> selectNetwork(UUID namespace, String name) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT id FROM `pixie-networks` WHERE namespace = ? AND name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, namespace.toString());
                statement.setString(2, name);

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(results.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
