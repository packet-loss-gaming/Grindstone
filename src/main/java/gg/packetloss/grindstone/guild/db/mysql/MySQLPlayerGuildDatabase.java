/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.db.mysql;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import gg.packetloss.grindstone.data.MySQLHandle;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.db.PlayerGuildDatabase;
import gg.packetloss.grindstone.guild.state.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class MySQLPlayerGuildDatabase implements PlayerGuildDatabase {
    private final Gson GUILD_SETTINGS_GSON = new Gson();

    private <T> Optional<T> tryLoadSettings(String settingsString, Class<T> settingsClass) {
        try {
            return Optional.of(GUILD_SETTINGS_GSON.fromJson(settingsString, settingsClass));
        } catch (JsonSyntaxException ex) {
            return Optional.empty();
        }
    }

    private Optional<InternalGuildState> buildGuildState(GuildType guildType, long experience, String settingsString) {
        switch (guildType) {
            case NINJA -> {
                NinjaStateSettings settings = tryLoadSettings(
                    settingsString,
                    NinjaStateSettings.class
                ).orElseGet(NinjaStateSettings::new);
                return Optional.of(new NinjaState(experience, settings));
            }
            case ROGUE -> {
                RogueStateSettings settings = tryLoadSettings(
                    settingsString,
                    RogueStateSettings.class
                ).orElseGet(RogueStateSettings::new);
                return Optional.of(new RogueState(experience, settings));
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<InternalGuildState> loadGuild(UUID playerID) {
        try (Connection connection = MySQLHandle.getConnection()) {
            String sql = "SELECT `guild-type`, `experience`, `settings` FROM `player-guilds` WHERE `player-id` = " +
                    "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1) AND `active` = true";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        GuildType guildType = GuildType.values()[results.getInt(1)];
                        long experience = results.getLong(2);
                        String settingsString = results.getString(3);

                        return buildGuildState(guildType, experience, settingsString);
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
            String sql = "SELECT `experience`, `settings` FROM `player-guilds` WHERE `player-id` = " +
                    "(SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1) AND `guild-type` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerID.toString());
                statement.setInt(2, type.ordinal());

                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        long experience = results.getLong(1);
                        String settingsString = results.getString(2);

                        return buildGuildState(type, experience, settingsString);
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
        String SQL = "INSERT INTO `player-guilds` (`player-id`, `guild-type`, `experience`, `settings`, `active`) " +
                "VALUES ((SELECT `playerid` FROM `lb-players` WHERE `lb-players`.`uuid` = ? LIMIT 1), ?, ?, ?, true) " +
                "ON DUPLICATE KEY UPDATE experience = values(experience), settings = values(settings), active = true";

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, playerID.toString());
            statement.setInt(2, guildState.getType().ordinal());
            statement.setLong(3, (long) guildState.getExperience());
            statement.setString(4, GUILD_SETTINGS_GSON.toJson(guildState.getSettings()));

            statement.execute();
        }
    }

    @Override
    public void updateActive(UUID playerID, InternalGuildState guildState) {
        try (Connection connection = MySQLHandle.getConnection()) {
            connection.setAutoCommit(false);

            deactivateGuilds(connection, playerID);
            updateGuild(connection, playerID, guildState);

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
