package gg.packetloss.grindstone.city.engine.pixieitems.db.mysql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gg.packetloss.grindstone.city.engine.pixieitems.db.PixieChestDatabase;
import gg.packetloss.grindstone.city.engine.pixieitems.db.PixieChestDefinition;
import gg.packetloss.grindstone.city.engine.pixieitems.db.PixieChestDetail;
import gg.packetloss.grindstone.city.engine.pixieitems.db.PixieNetworkDefinition;
import gg.packetloss.grindstone.data.MySQLHandle;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static gg.packetloss.grindstone.util.DBUtil.preparePlaceHolders;
import static gg.packetloss.grindstone.util.DBUtil.setIntValues;

public class MySQLPixieChestDatabase implements PixieChestDatabase {
    private boolean addChests(int networkID, String itemNames, Location... locations) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "INSERT INTO `pixie-chests` (`network-id`, `world`, `x`, `y`, `z`, `cx`, `cz`, `item-names`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Location loc : locations) {
                    statement.setInt(1, networkID);
                    statement.setString(2, loc.getWorld().getName());
                    statement.setInt(3, loc.getBlockX());
                    statement.setInt(4, loc.getBlockY());
                    statement.setInt(5, loc.getBlockZ());
                    statement.setInt(6, loc.getBlockX() >> 4);
                    statement.setInt(7, loc.getBlockZ() >> 4);
                    statement.setString(8, itemNames);
                    statement.addBatch();
                }

                statement.executeBatch();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean addSource(int networkID, Location... locations) {
        return addChests(networkID, null, locations);
    }

    @Override
    public boolean addSink(int networkID, Set<String> itemNames, Location... locations) {
        return addChests(networkID, new Gson().toJson(itemNames), locations);
    }

    private String getChestDetailLookupSQL(Location location) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT `network-id`, `item-names` FROM `pixie-chests` WHERE ");
        appendBlockPlaceHolders(sqlBuilder, location);
        return sqlBuilder.toString();
    }

    @Override
    public Optional<PixieChestDetail> getDetailsAtLocation(Location location) {
        try (Connection connection = MySQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getChestDetailLookupSQL(location))) {
                setBlockPlaceHolderParams(0, statement, location);

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        Gson gson = new Gson();
                        Type itemNamesSetType = new TypeToken<HashSet<String>>() { }.getType();

                        HashSet<String> itemNames = gson.fromJson(results.getString(2), itemNamesSetType);
                        return Optional.of(new PixieChestDetail(results.getInt(1), itemNames));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private void appendBlockPlaceHolders(StringBuilder builder, Location... locations) {
        for (int i = 0; i < locations.length; ++i) {
            builder.append("(x = ? AND y = ? AND z = ?)");
            if (i != locations.length - 1) {
                builder.append(" OR ");
            }
        }
    }

    private String getChestDeleteSQL(Location[] locations) {
        StringBuilder sqlBuilder = new StringBuilder("DELETE FROM `pixie-chests` WHERE `network-id` = ? AND (");
        appendBlockPlaceHolders(sqlBuilder, locations);
        sqlBuilder.append(')');
        return sqlBuilder.toString();
    }

    private void setBlockPlaceHolderParams(int startingOffset, PreparedStatement statement, Location... locations) throws SQLException {
        for (int i = 0; i < locations.length; ++i) {
            int rowOffset = i * 3;

            statement.setInt(rowOffset + startingOffset + 1, locations[i].getBlockX());
            statement.setInt(rowOffset + startingOffset + 2, locations[i].getBlockY());
            statement.setInt(rowOffset + startingOffset + 3, locations[i].getBlockZ());
        }
    }

    @Override
    public Optional<Integer> removeChest(int networkID, Location... locations) {
        try (Connection connection = MySQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getChestDeleteSQL(locations))) {
                statement.setInt(1, networkID);
                setBlockPlaceHolderParams(1, statement, locations);

                return Optional.of(statement.executeUpdate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private String getChestNetworkLookupSQL(Location[] locations) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT `network-id` FROM `pixie-chests` WHERE ");
        appendBlockPlaceHolders(sqlBuilder, locations);
        return sqlBuilder.toString();
    }

    @Override
    public Optional<Collection<Integer>> getNetworksInLocations(Location... locations) {
        try (Connection connection = MySQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getChestNetworkLookupSQL(locations))) {
                setBlockPlaceHolderParams(0, statement, locations);

                try (ResultSet results = statement.executeQuery()) {
                    List<Integer> networkIds = new ArrayList<>();

                    while (results.next()) {
                        networkIds.add(results.getInt(1));
                    }

                    return Optional.of(networkIds);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<Collection<Integer>> getNetworksInChunk(Chunk chunk) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT DISTINCT `network-id` FROM `pixie-chests` WHERE `cx` = ? AND `cz` = ? AND `item-names` IS NULL";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, chunk.getX());
                statement.setInt(2, chunk.getZ());

                try (ResultSet results = statement.executeQuery()) {
                    List<Integer> networkIds = new ArrayList<>();

                    while (results.next()) {
                        networkIds.add(results.getInt(1));
                    }

                    return Optional.of(networkIds);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<Collection<PixieNetworkDefinition>> getChestsInNetworks(List<Integer> networks) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `network-id`, `world`, `x`, `y`, `z`, `item-names` FROM `pixie-chests` WHERE `network-id` IN ("
                    + preparePlaceHolders(networks.size()) +")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setIntValues(statement, networks);

                try (ResultSet results = statement.executeQuery()) {
                    Map<Integer, PixieNetworkDefinition> networkIds = new HashMap<>();

                    Gson gson = new Gson();
                    Type itemNamesSetType = new TypeToken<HashSet<String>>() { }.getType();

                    while (results.next()) {
                        int networkID = results.getInt(1);

                        PixieNetworkDefinition definition = networkIds.compute(networkID, (ignored, value) -> {
                            if (value == null) {
                                return new PixieNetworkDefinition(networkID);
                            }
                            return value;
                        });

                        HashSet<String> itemNames = gson.fromJson(results.getString(6), itemNamesSetType);

                        definition.getChests().add(new PixieChestDefinition(
                                results.getString(2),
                                results.getInt(3),
                                results.getInt(4),
                                results.getInt(5),
                                itemNames
                        ));
                    }

                    return Optional.of(networkIds.values());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
