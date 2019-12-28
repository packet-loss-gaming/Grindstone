package gg.packetloss.grindstone.guild.db.mysql;

import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.db.PlayerGuildDatabase;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.NinjaState;
import gg.packetloss.grindstone.guild.state.RogueState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class MySQLPlayerGuildDatabase implements PlayerGuildDatabase {
    @Override
    public Optional<InternalGuildState> loadGuild(UUID playerID) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `guild-type`, `experience` FROM `player-guilds` WHERE `player-id` = " +
                    "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1) AND `active` = true";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        GuildType guildType = GuildType.values()[results.getInt(1)];
                        long experience = results.getLong(2);

                        switch (guildType) {
                            case NINJA:
                                return Optional.of(new NinjaState(experience));
                            case ROGUE:
                                return Optional.of(new RogueState(experience));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<InternalGuildState> loadGuild(UUID playerID, GuildType type) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `experience` FROM `player-guilds` WHERE `player-id` = " +
                    "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1) AND `guild-type` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());
                statement.setInt(2, type.ordinal());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        long experience = results.getLong(1);

                        switch (type) {
                            case NINJA:
                                return Optional.of(new NinjaState(experience));
                            case ROGUE:
                                return Optional.of(new RogueState(experience));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private void deactivateGuilds(Connection connection, UUID playerID) throws SQLException {
        String SQL = "UPDATE `player-guilds` SET `active` = false WHERE `player-id` = " +
                "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1)";

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());

            statement.execute();
        }
    }

    private void updateGuild(Connection connection, UUID playerID, InternalGuildState guildState) throws SQLException {
        String SQL = "INSERT INTO `player-guilds` (`player-id`, `guild-type`, `experience`, `active`) " +
                "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?, true) " +
                "ON DUPLICATE KEY UPDATE experience = values(experience), active = true";

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());
            statement.setInt(2, guildState.getType().ordinal());
            statement.setLong(3, guildState.getExperience());

            statement.execute();
        }
    }

    @Override
    public void updateActive(UUID playerID, InternalGuildState guildState) {
        try (Connection connection = MySQLHandle.getConnection()) {
            deactivateGuilds(connection, playerID);
            updateGuild(connection, playerID, guildState);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
