/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.authentication;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.*;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import static org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Auth", desc = "Authentication System for Shivtr")
public class AuthComponent extends BukkitComponent implements Listener, Runnable {


    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();
    private final Logger log = inst.getLogger();

    private static Permission permission = null;
    private LocalConfiguration config;
    //private ConcurrentHashMap<String, Character> characters = new ConcurrentHashMap<>();
    private Set<String> characters = new CopyOnWriteArraySet<>();


    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        registerCommands(Commands.class);
        if (config.whitelist) {
            //noinspection AccessStaticViaInstance
            inst.registerEvents(new WhiteList());
        } else {
            //noinspection AccessStaticViaInstance
            inst.registerEvents(new GreyList());
            setupPermissions();
        }
        if (config.updateFrequency < 30) {
            config.updateFrequency = 30;
            log.warning("The update frequency was set at: " + config.updateFrequency + " minutes"
                    + " and must be at least 30 minutes.");
        }
        server.getScheduler().runTaskTimerAsynchronously(inst, this, 0, 20 * 60 * config.updateFrequency);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("white-list")
        public boolean whitelist = true;
        @Setting("website-url")
        public String websiteUrl = "http://example.shivtr.com/";
        @Setting("update-frequency")
        public int updateFrequency = 30;
        @Setting("grey-list-group")
        public String greyListGroup = "Member";
    }

    @Override
    public synchronized void run() {

        JSONArray charactersL = getFrom("characters.json");

        log.info("Testing the connection to " + config.websiteUrl + "...");
        if (charactersL != null) {
            log.info("Connection test successful.");
            if (!charactersL.isEmpty()) {
                updateWhiteList(charactersL);
            } else {
                log.warning("No characters could be downloaded!");
                log.info("Your website could be under maintenance or contain no characters.");
            }
        } else {
            log.warning("Connection test failed!");
        }

        if (characters.isEmpty()) {
            log.info("Attempting to load offline files...");
            loadBackupWhiteList();
        }

        log.info(characters.size() + " characters have been loaded.");
    }

    private boolean setupPermissions() {

        RegisteredServiceProvider<Permission> permissionProvider = server.getServicesManager().getRegistration(net
                .milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) permission = permissionProvider.getProvider();

        return (permission != null);
    }

    public class WhiteList implements Listener {

        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {

            try {
                if (!isListed(event.getName()) && event.getLoginResult().equals(Result.ALLOWED)) {
                    event.disallow(Result.KICK_WHITELIST, "You must register on " +
                            "your account on " + config.websiteUrl + ".");
                }
            } catch (Exception e) {
                event.disallow(Result.KICK_WHITELIST, "An error has occurred please try again in a few minutes.");
            }
        }
    }

    public class GreyList implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerLogin(PlayerJoinEvent event) {

            Player player = event.getPlayer();
            try {
                if (permission == null || inst.hasPermission(player, "aurora.auth.member")) return;

                if (isListed(player.getName())) {
                    permission.playerAddGroup((World) null, player.getName(), config.greyListGroup);
                    ChatUtil.sendNotice(player, "Thank you for registering your account.");
                } else {
                    ChatUtil.sendWarning(player, "You are currently a probationary player.");
                    ChatUtil.sendWarning(player, "To get full access please register your account at: " + config.websiteUrl + ".");
                }
            } catch (Exception e) {
                player.kickPlayer("An error has occurred please try again in a few minutes.");
            }
        }
    }

    public class Commands {

        @Command(aliases = {"auth"}, desc = "Commands for the Authentication System.",
                min = 0, max = 0)
        @NestedCommand({AuthCommands.class})
        public void authCmd(CommandContext args, CommandSender sender) {

        }

    }

    public class AuthCommands {

        /*
        @Command(aliases = {"login"}, desc = "Verify with Shivtr",
                min = 2, max = 2)
        public void authLoginCmd(CommandContext args, CommandSender sender) throws CommandException{

            Player player = PlayerUtil.checkPlayer(sender);


            String authToke = authenticate(sender.getName(), args.getString(0), args.getString(1));

            if (!authToke.equals("401")) {
                ChatUtil.sendNotice(sender, "Successfully authenticated with shivtr.");

                AuthenticatedShivtrCharacter shivtrCharacter
                        = new AuthenticatedShivtrCharacter(sender.getName(), authToke);
                playerAuth.put(sender.getName(), shivtrCharacter);

                if (promotedPlayers.contains(sender)) promotedPlayers.remove(sender);
            } else if (promotedPlayers.contains(sender) && config.kickOnFailedPrompt){
                ((Player) sender).kickPlayer("Failed authentication.");
            } else {
                ChatUtil.sendWarning(sender, "Authentication failed.");
            }
        }
        */

        @Command(aliases = {"update"}, desc = "Update the auth file",
                min = 0, max = 0)
        @CommandPermissions("aurora.auth.update")
        public void authUpdateCmd(CommandContext args, CommandSender sender) throws CommandException {

            run();
            if (sender instanceof Player) ChatUtil.sendNotice(sender, "The Characters List(s) is now being updated.");
        }
    }

    public synchronized boolean isListed(String playerName) {

        return characters.contains(playerName.trim().toLowerCase());
    }

    public synchronized JSONArray getFrom(String subAddress) {

        JSONArray objective = new JSONArray();
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            JSONParser parser = new JSONParser();
            for (int i = 1; true; i++) {

                try {
                    // Establish the connection
                    URL url = new URL(config.websiteUrl + subAddress + "?page=" + i);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(1500);
                    connection.setReadTimeout(1500);

                    // Check response codes return if invalid
                    if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;

                    // Begin to read results
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    // Parse Data
                    JSONObject o = (JSONObject) parser.parse(builder.toString());
                    JSONArray ao = (JSONArray) o.get("characters");
                    if (ao.isEmpty()) break;
                    Collections.addAll(objective, (JSONObject[]) ao.toArray(new JSONObject[ao.size()]));
                } catch (ParseException e) {
                    break;
                } finally {
                    if (connection != null) connection.disconnect();

                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }

        return objective;
    }

    private static final FilenameFilter filenameFilter = (dir, name) -> name.startsWith("character") && name.endsWith(".json");

    public synchronized void updateWhiteList(JSONArray object) {

        // Load the storage directory
        File charactersDirectory = new File(inst.getDataFolder().getPath() + "/characters");
        if (!charactersDirectory.exists()) charactersDirectory.mkdir();
        log.info("Updating white list...");

        if (!loadCharacters((JSONObject[]) object.toArray(new JSONObject[object.size()]))) {
            log.info("No changes detected.");
            return;
        }

        // Remove outdated JSON backup files
        for (File file : charactersDirectory.listFiles(filenameFilter)) {
            file.delete();
        }

        // Create new JSON backup file
        BufferedWriter out = null;

        File characterList = new File(charactersDirectory, "character-list.json");
        try {
            if (characterList.createNewFile()) {
                out = new BufferedWriter(new FileWriter(characterList));
                out.write(object.toJSONString());
            } else {
                log.warning("Could not create the new character list offline file!");
            }
        } catch (IOException ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }

        log.info("The white list has updated successfully.");
    }

    private synchronized void loadBackupWhiteList() {

        File charactersDirectory = new File(inst.getDataFolder().getPath() + "/characters");
        File characterFile = new File(charactersDirectory, "character-list.json");
        if (!characterFile.exists()) {
            log.warning("No offline file found!");
            return;
        }

        BufferedReader reader = null;
        JSONParser parser = new JSONParser();

        try {
            reader = new BufferedReader(new FileReader(characterFile));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONArray characterArray = ((JSONArray) parser.parse(builder.toString()));
            loadCharacters((JSONObject[]) characterArray.toArray(new JSONObject[characterArray.size()]));
        } catch (IOException e) {
            log.warning("Could not read file: " + characterFile.getName() + ".");
        } catch (ParseException p) {
            log.warning("Could not parse file: " + characterFile.getName() + ".");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        log.info("The offline file has been loaded.");
    }

    public synchronized boolean loadCharacters(JSONObject[] chars) {

        boolean diff = false;
        Set<String> localChars = new HashSet<>();
        for (JSONObject aChar : chars) {
            String targ = aChar.get("name").toString().trim().toLowerCase();
            if (!characters.contains(targ)) {
                diff = true;
            }
            localChars.add(targ);
        }

        diff = diff || characters.size() != localChars.size();

        if (diff) {
            // Clear all old chars
            characters.clear();

            // Add all new Characters
            characters.addAll(localChars);
            return true;
        }
        return false;
    }
}
