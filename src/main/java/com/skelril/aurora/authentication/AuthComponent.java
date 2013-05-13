package com.skelril.aurora.authentication;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.*;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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

    private LocalConfiguration config;
    private ConcurrentHashMap<String, Character> characters = new ConcurrentHashMap<>();


    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
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

        @Setting("website-url")
        public String websiteUrl = "http://example.shivtr.com/";
        @Setting("update-frequency")
        public int updateFrequency = 30;
        @Setting("prompt-when-offline-mode")
        public boolean offlineEnabled = false;
        @Setting("kick-on-failed-prompt")
        public boolean kickOnFailedPrompt = true;
    }

    @Override
    public synchronized void run() {

        JSONArray[] objects = getFrom("characters.json");

        log.info("Testing the connection to " + config.websiteUrl + "...");
        if (objects != null) {
            log.info("Connection test successful.");
            if (objects.length > 0) {
                updateWhiteList(objects);
            } else {
                log.warning("No characters could be found!");
                log.info("Your website could be under maintenance or contain no characters.");
            }
        } else {
            log.warning("Connection test failed!");
            if (characters.size() == 0) {
                log.info("Attempting to load offline files...");
                loadBackupWhiteList();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {

        try {
            if (!canJoin(event.getName()) && event.getLoginResult().equals(Result.ALLOWED)) {
                event.disallow(Result.KICK_WHITELIST, "You must register on " +
                        "your account on " + config.websiteUrl + ".");
            }
        } catch (Exception e) {
            event.disallow(Result.KICK_WHITELIST, "An error has occurred please try again in a few minutes.");
        }
    }

    /*
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (!server.getOnlineMode() && config.offlineEnabled) {
            promotedPlayers.add(event.getPlayer());
        }
    }
    */

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

    public synchronized boolean canJoin(String playerName) {

        return characters.keySet().contains(playerName.trim().toLowerCase());
    }

    public synchronized JSONArray[] getFrom(String subAddress) {

        JSONArray objective[] = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {

            List<JSONArray> objects = new ArrayList<>();
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
                    JSONArray o = (JSONArray) parser.parse(builder.toString());
                    if (o.isEmpty()) break;
                    objects.add(o);

                } catch (ParseException e) {
                    break;
                }
            }
            objective = objects.toArray(new JSONArray[objects.size()]);
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

    public synchronized void updateWhiteList(JSONArray[] object) {

        // Load the storage directory
        File charactersDirectory = new File(inst.getDataFolder().getPath() + "/characters");
        if (!charactersDirectory.exists()) charactersDirectory.mkdir();
        log.info("Updating white list.");

        // Remove outdated JSON backup files
        for (File file : charactersDirectory.listFiles(filenameFilter)) {
            log.info("Removed file: " + file.getName() + ".");
            file.delete();
        }

        // Create new JSON backup files
        int fileNumber = 1;
        for (JSONArray aJSONArray : object) {

            BufferedWriter out = null;

            File f = new File(charactersDirectory, "character-list-" + fileNumber + ".json");
            try {
                if (f.createNewFile()) {
                    out = new BufferedWriter(new FileWriter(f));
                    out.write(aJSONArray.toJSONString());
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
            fileNumber++;

            addCharacters(aJSONArray);
        }

        log.info("The white list has updated successfully.");
    }

    private synchronized void loadBackupWhiteList() {

        File charactersDirectory = new File(inst.getDataFolder().getPath() + "/characters");
        if (!charactersDirectory.exists()) {
            log.warning("No offline files found!");
            return;
        }

        BufferedReader reader = null;
        JSONParser parser = new JSONParser();

        for (File file : charactersDirectory.listFiles(filenameFilter)) {
            try {
                log.info("Found file: " + file.getName() + ".");
                reader = new BufferedReader(new FileReader(file));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                addCharacters((JSONArray) parser.parse(builder.toString()));
            } catch (IOException e) {
                log.warning("Could not read file: " + file.getName() + ".");
            } catch (ParseException p) {
                log.warning("Could not parse file: " + file.getName() + ".");
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        log.info("All found offline files have been loaded.");
    }

    public synchronized void addCharacters(JSONArray aJSONArray) {

        // Remove Old Characters
        characters.clear();

        // Add all new Characters
        for (Object aCharacterObject : aJSONArray) {
            JSONObject aJSONCharacterObject = (JSONObject) aCharacterObject;
            characters.put(aJSONCharacterObject.get("name").toString().trim().toLowerCase(),
                    new Character(aJSONCharacterObject.get("name").toString()));
        }
    }
}
