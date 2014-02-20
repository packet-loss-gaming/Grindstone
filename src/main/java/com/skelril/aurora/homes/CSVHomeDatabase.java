/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.homes;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.entity.Player;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CSVHomeDatabase implements HomeDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final File homeFile;

    /**
     * Used to lookup inmates by name
     */
    protected Map<String, Home> nameHome = new HashMap<>();

    /**
     * A set of all inmates
     */
    protected final Set<Home> homes = new HashSet<>();

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public CSVHomeDatabase(String name, File storageDir) {

        homeFile = new File(storageDir, name + ".csv");
    }

    @Override
    public synchronized boolean load() {

        FileInputStream input = null;
        boolean successful = true;

        try {
            input = new FileInputStream(homeFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 5) {
                    log.warning("A home entry with < 5 fields was found!");
                    continue;
                }
                try {
                    String name = line[0].toLowerCase();
                    String world = line[1];
                    int x = Integer.parseInt(line[2]);
                    int y = Integer.parseInt(line[3]);
                    int z = Integer.parseInt(line[4]);

                    if ("".equals(name) || "null".equals(name)) name = null;

                    Home home = new Home(name, world, x, y, z);

                    if (name != null) nameHome.put(name, home);
                    homes.add(home);
                } catch (NumberFormatException e) {
                    log.warning("Incorrect int found in home entry!");
                } catch (Exception e) {
                    log.warning("The home's world could not be found!");
                }
            }
            log.info(homes.size() + " houses loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            nameHome = new HashMap<>();
            log.warning("Failed to load " + homeFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
        return successful;
    }

    @Override
    public synchronized boolean save() {

        FileOutputStream output = null;
        boolean successful = true;

        try {
            output = new FileOutputStream(homeFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (Home home : homes) {
                line = new String[]{
                        home.getPlayerName().toLowerCase(),
                        home.getWorldName(),
                        String.valueOf(home.getX()),
                        String.valueOf(home.getY()),
                        String.valueOf(home.getZ())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + homeFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
        return successful;
    }

    @Override
    public boolean houseExist(String name) {

        return nameHome.containsKey(name.toLowerCase());
    }

    @Override
    public void saveHouse(Player player, String world, int x, int y, int z) {

        Home home = new Home(player.getName().toLowerCase(), world, x, y, z);
        nameHome.put(player.getName().toLowerCase(), home);
        homes.add(home);
    }

    @Override
    public boolean deleteHouse(String player) {

        Home home = null;
        if (nameHome.containsKey(player.toLowerCase())) {
            home = nameHome.remove(player.toLowerCase());
        }

        if (home != null) {
            homes.remove(home);
            return true;
        }
        return false;
    }

    @Override
    public Home getHouse(String name) {

        return nameHome.containsKey(name.toLowerCase()) ? nameHome.get(name.toLowerCase()) : null;
    }
}
