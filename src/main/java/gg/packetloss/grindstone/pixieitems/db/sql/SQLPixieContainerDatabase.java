/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db.sql;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.pixieitems.db.PixieChestDefinition;
import gg.packetloss.grindstone.pixieitems.db.PixieChestDetail;
import gg.packetloss.grindstone.pixieitems.db.PixieContainerDatabase;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDefinition;
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

public class SQLPixieContainerDatabase implements PixieContainerDatabase {
    private static final Gson GSON = new Gson();

    private boolean addChests(int networkID, String itemMapping, Location... locations) {
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                INSERT INTO minecraft.pixie_chests (network_id, world, x, y, z, cx, cz, item_names)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Location loc : locations) {
                    statement.setInt(1, networkID);
                    statement.setString(2, loc.getWorld().getName());
                    statement.setInt(3, loc.getBlockX());
                    statement.setInt(4, loc.getBlockY());
                    statement.setInt(5, loc.getBlockZ());
                    statement.setInt(6, loc.getBlockX() >> 4);
                    statement.setInt(7, loc.getBlockZ() >> 4);
                    statement.setString(8, itemMapping);
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
    public boolean addSink(int networkID, Map<String, List<Integer>> itemMapping, Location... locations) {
        return addChests(networkID, GSON.toJson(itemMapping), locations);
    }

    private String getChestDetailLookupSQL(Location location) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT network_id, item_names FROM minecraft.pixie_chests WHERE "
        );
        appendBlockPlaceHolders(sqlBuilder, location);
        return sqlBuilder.toString();
    }

    private Map<String, List<Integer>> deserializeItemMapping(String itemMappingJson) {
        try {
            Type itemNamesSetType = new TypeToken<HashMap<String, List<Integer>>>() { }.getType();
            return GSON.fromJson(itemMappingJson, itemNamesSetType);
        } catch (JsonParseException ex) {
            Type itemNamesSetType = new TypeToken<HashSet<String>>() { }.getType();
            Set<String> itemNames = GSON.fromJson(itemMappingJson, itemNamesSetType);

            Map<String, List<Integer>> itemMapping = new HashMap<>();
            for (String itemName : itemNames) {
                itemMapping.put(itemName, new ArrayList<>());
            }
            return itemMapping;
        }
    }

    @Override
    public Optional<PixieChestDetail> getDetailsAtLocation(Location location) {
        try (Connection connection = SQLHandle.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getChestDetailLookupSQL(location))) {
                setBlockPlaceHolderParams(0, statement, location);

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        Map<String, List<Integer>> itemMapping = deserializeItemMapping(results.getString(2));
                        return Optional.of(new PixieChestDetail(results.getInt(1), itemMapping));
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
            builder.append("(world = ? AND x = ? AND y = ? AND z = ?)");
            if (i != locations.length - 1) {
                builder.append(" OR ");
            }
        }
    }

    private String getChestDeleteSQL(Location[] locations) {
        StringBuilder sqlBuilder = new StringBuilder("DELETE FROM minecraft.pixie_chests WHERE network_id = ? AND (");
        appendBlockPlaceHolders(sqlBuilder, locations);
        sqlBuilder.append(')');
        return sqlBuilder.toString();
    }

    private void setBlockPlaceHolderParams(int startingOffset, PreparedStatement statement,
                                           Location... locations) throws SQLException {
        for (int i = 0; i < locations.length; ++i) {
            int rowOffset = i * 3;

            statement.setString(rowOffset + startingOffset + 1, locations[i].getWorld().getName());
            statement.setInt(rowOffset + startingOffset + 2, locations[i].getBlockX());
            statement.setInt(rowOffset + startingOffset + 3, locations[i].getBlockY());
            statement.setInt(rowOffset + startingOffset + 4, locations[i].getBlockZ());
        }
    }

    @Override
    public Optional<Integer> removeContainer(int networkID, Location... locations) {
        try (Connection connection = SQLHandle.getConnection()) {
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
        StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT network_id FROM minecraft.pixie_chests WHERE ");
        appendBlockPlaceHolders(sqlBuilder, locations);
        return sqlBuilder.toString();
    }

    @Override
    public Optional<Collection<Integer>> getNetworksInLocations(Location... locations) {
        try (Connection connection = SQLHandle.getConnection()) {
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
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT DISTINCT network_id FROM minecraft.pixie_chests
                    WHERE
                        world = ?
                    AND
                        cx = ?
                    AND
                        cz = ?
                    AND
                        item_names IS NULL
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, chunk.getWorld().getName());
                statement.setInt(2, chunk.getX());
                statement.setInt(3, chunk.getZ());

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
        try (Connection connection = SQLHandle.getConnection()) {
            String sql = """
                SELECT network_id, world, x, y, z, item_names FROM minecraft.pixie_chests WHERE network_id IN (
            """;
            sql += preparePlaceHolders(networks.size()) + ')';

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setIntValues(statement, networks);

                try (ResultSet results = statement.executeQuery()) {
                    Map<Integer, PixieNetworkDefinition> networkIds = new HashMap<>();

                    while (results.next()) {
                        int networkID = results.getInt(1);

                        PixieNetworkDefinition definition = networkIds.compute(networkID, (ignored, value) -> {
                            return Objects.requireNonNullElseGet(value, () -> new PixieNetworkDefinition(networkID));
                        });

                        Map<String, List<Integer>> itemMapping = deserializeItemMapping(results.getString(6));
                        definition.getChests().add(new PixieChestDefinition(
                                results.getString(2),
                                results.getInt(3),
                                results.getInt(4),
                                results.getInt(5),
                                itemMapping
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
