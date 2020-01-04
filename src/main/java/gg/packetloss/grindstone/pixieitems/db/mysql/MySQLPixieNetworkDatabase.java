package gg.packetloss.grindstone.pixieitems.db.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDatabase;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySQLPixieNetworkDatabase implements PixieNetworkDatabase {
    @Override
    public Optional<PixieNetworkDetail> createNetwork(UUID namespace, String name) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "INSERT INTO `pixie-networks` (namespace, name) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, namespace.toString());
                statement.setString(2, name);
                statement.execute();

                try (ResultSet results = statement.getGeneratedKeys()) {
                    if (results.next()) {
                        return Optional.of(new PixieNetworkDetail(results.getInt(1), namespace, name));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<PixieNetworkDetail> selectNetwork(UUID namespace, String name) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT id FROM `pixie-networks` WHERE namespace = ? AND name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, namespace.toString());
                statement.setString(2, name);

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(new PixieNetworkDetail(results.getInt(1), namespace, name));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<PixieNetworkDetail> selectNetwork(int networkID) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT namespace, name FROM `pixie-networks` WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, networkID);

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return Optional.of(new PixieNetworkDetail(
                                networkID,
                                UUID.fromString(results.getString(1)),
                                results.getString(2)
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<PixieNetworkDetail> selectNetworks(UUID namespace) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT id, name FROM `pixie-networks` WHERE namespace = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, namespace.toString());

                try (ResultSet results = statement.executeQuery()) {
                    List<PixieNetworkDetail> networks = new ArrayList<>();
                    while (results.next()) {
                        networks.add(new PixieNetworkDetail(
                                results.getInt(1),
                                namespace,
                                results.getString(2)
                        ));
                    }
                    return networks;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return List.of();
    }
}
