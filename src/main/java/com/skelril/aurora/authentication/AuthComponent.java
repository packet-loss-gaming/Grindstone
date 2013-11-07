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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentHashMap<String, Character> characters = new ConcurrentHashMap<>();


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

        JSONArray characters = getFrom("characters.json");

        log.info("Testing the connection to " + config.websiteUrl + "...");
        if (characters != null) {
            log.info("Connection test successful.");
            if (characters.size() > 0) {
                updateWhiteList(characters);
            } else {
                log.warning("No characters could be downloaded!");
                log.info("Your website could be under maintenance or contain no characters.");
            }
        } else {
            log.warning("Connection test failed!");
        }

        if (this.characters.size() == 0) {
            log.info("Attempting to load offline files...");
            loadBackupWhiteList();
        }

        log.info(this.characters.size() + " characters have been loaded.");
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

            if (!(sender instanceof Player))
                throw new CommandException("You must be a player to use this command.");

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

        return characters.keySet().contains(playerName.trim().toLowerCase());
    }

    public synchronized JSONArray getFrom(String subAddress) {

        JSONArray objective = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {

            List<JSONObject> objects = new ArrayList<>();
            JSONParser parser = new JSONParser();
            for (int i = 1; true; i++) {

                try {
                    // Establish the connection
                    URL url = new URL(config.websiteUrl + subAddress + "?game_id=11?page=" + i);
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
                    Collections.addAll(objects, (JSONObject[]) ao.toArray(new JSONObject[ao.size()]));

                } catch (ParseException e) {
                    break;
                }
            }
            objective = new JSONArray();
            objective.addAll(objects);
        } catch (IOException e) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        return objective;
    }

    private static final FilenameFilter filenameFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {

            return name.startsWith("character") && name.endsWith(".json");
        }
    };

    public synchronized void updateWhiteList(JSONArray object) {

        // Load the storage directory
        File charactersDirectory = new File(inst.getDataFolder().getPath() + "/characters");
        if (!charactersDirectory.exists()) charactersDirectory.mkdir();
        log.info("Updating white list...");

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

        loadCharacters((JSONObject[]) object.toArray(new JSONObject[object.size()]));

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

    public synchronized void loadCharacters(JSONObject[] characters) {

        // Remove Old Characters
        this.characters.clear();

        // Add all new Characters
        for (JSONObject aCharacter : characters) {
            this.characters.put(aCharacter.get("name").toString().trim().toLowerCase(),
                    new Character(aCharacter.get("name").toString()));
        }
    }
}
