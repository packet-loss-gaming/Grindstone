/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.jail;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CSVJailCellDatabase implements JailCellDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final Logger auditLogger
            = Logger.getLogger("Minecraft.CommandBook.Jail");
    protected final File cellFile;

    /**
     * Used to lookup cells by name
     */
    protected Map<String, Map<String, JailCell>> nameJailCell = new HashMap<>();

    /**
     * A set of all cells
     */
    protected final Set<JailCell> jailCells = new HashSet<>();

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public CSVJailCellDatabase(File cellStorageDir) {

        cellFile = new File(cellStorageDir, "cells.csv");

        // Set up an audit trail
        try {
            FileHandler handler = new FileHandler(
                    (new File(cellStorageDir, "cells.%g.%u.log")).getAbsolutePath()
                            .replace("\\", "/"), true
            );

            handler.setFormatter(new java.util.logging.Formatter() {

                @Override
                public String format(LogRecord record) {

                    return "[" + dateFormat.format(new Date()) + "] " + record.getMessage() + "\r\n";
                }
            });

            auditLogger.addHandler(handler);
        } catch (SecurityException | IOException e) {
            log.warning("Failed to setup audit log for the CSV cell database: " + e.getMessage());
        }
    }

    @Override
    public synchronized boolean load() {

        FileInputStream input = null;
        boolean successful = true;

        try {
            input = new FileInputStream(cellFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 6) {
                    log.warning("A cell entry with < 6 fields was found!");
                    continue;
                }
                try {
                    String name = line[0].trim().toLowerCase();
                    String prison = line[1].trim().toLowerCase();
                    String world = line[2].trim();
                    int x = Integer.parseInt(line[3]);
                    int y = Integer.parseInt(line[4]);
                    int z = Integer.parseInt(line[5]);
                    if ("".equals(name) || "null".equals(name)) name = null;
                    if (name == null) continue;
                    JailCell jailCell = new JailCell(name, prison, world, x, y, z);
                    Map<String, JailCell> map = nameJailCell.get(prison);
                    if (map == null) {
                        map = new HashMap<>();
                        nameJailCell.put(prison, map);
                    }
                    map.put(name, jailCell);
                    jailCells.add(jailCell);
                } catch (NumberFormatException e) {
                    log.warning("Non-int int field found in cell!");
                }
            }
            log.info(jailCells.size() + " jail cell(s) loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            nameJailCell = new HashMap<>();
            log.warning("Failed to load " + cellFile.getAbsolutePath() + ": " + e.getMessage());
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
            output = new FileOutputStream(cellFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (JailCell jailCell : jailCells) {
                line = new String[]{
                        jailCell.getCellName().trim().toLowerCase(),
                        jailCell.getPrisonName().trim().toLowerCase(),
                        jailCell.getWorldName().trim(),
                        String.valueOf(jailCell.getX()),
                        String.valueOf(jailCell.getY()),
                        String.valueOf(jailCell.getZ())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + cellFile.getAbsolutePath() + ": " + e.getMessage());
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
    public boolean unload() {

        for (Handler handler : auditLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                handler.flush();
                handler.close();
                auditLogger.removeHandler(handler);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean cellExist(String prison, String name) {

        Map<String, JailCell> map = nameJailCell.get(prison.trim().toLowerCase());
        return map != null && map.get(name.trim().toLowerCase()) != null;
    }

    @Override
    public void createJailCell(String prisonName, String cellName, CommandSender source, Location location) {

        prisonName = prisonName.trim().toLowerCase();
        cellName = cellName.trim().toLowerCase();

        JailCell jailCell = new JailCell(cellName, prisonName,
                location.getWorld().getName().trim(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());

        Map<String, JailCell> map = nameJailCell.get(prisonName);
        if (map == null) {
            map = new HashMap<>();
            nameJailCell.put(prisonName, map);
        }
        map.put(cellName, jailCell);
        jailCells.add(jailCell);
        auditLogger.info(String.format("CELL: %s created cell: %s",
                source == null ? "Plugin" : com.sk89q.commandbook.util.ChatUtil.toUniqueName(source),
                cellName));
    }

    @Override
    public boolean deleteJailCell(String prisonName, String cellName, CommandSender source) {

        Validate.notNull(prisonName);
        Validate.notNull(cellName);

        prisonName = prisonName.trim().toLowerCase();
        cellName = cellName.trim().toLowerCase();

        JailCell jailCell;

        Map<String, JailCell> map = nameJailCell.get(prisonName);
        if (map == null) return false; // No prison

        jailCell = map.remove(cellName);
        if (jailCell != null) {

            if (map.size() < 1) {
                nameJailCell.remove(prisonName);
            }

            jailCells.remove(jailCell);
            auditLogger.info(String.format("CELL: %s removed cell: %s",
                    source == null ? "Plugin" : ChatUtil.toUniqueName(source),
                    cellName));
            return true;
        }
        return false;
    }

    @Override
    public JailCell getJailCell(String prisonName, String cellName) {

        Map<String, JailCell> map = nameJailCell.get(prisonName.trim().toLowerCase());
        return map == null ? null : map.get(cellName.trim().toLowerCase());
    }

    @Override
    public boolean prisonExist(String prisonName) {

        return nameJailCell.containsKey(prisonName.trim().toLowerCase());
    }

    @Override
    public List<JailCell> getPrison(String prisonName) {

        Map<String, JailCell> map = nameJailCell.get(prisonName.trim().toLowerCase());
        return map == null ? null : Lists.newArrayList(map.values());
    }

    @Override
    public List<String> getPrisons() {

        return Lists.newArrayList(nameJailCell.keySet());
    }

    @Override
    public List<JailCell> getJailCells() {

        return Lists.newArrayList(jailCells);
    }

    @Override
    public Iterator<JailCell> iterator() {

        return new Iterator<JailCell>() {

            private final Iterator<JailCell> setIter = jailCells.iterator();
            private JailCell next;

            public boolean hasNext() {

                return setIter.hasNext();
            }

            public JailCell next() {

                return next = setIter.next();
            }

            public void remove() {

                deleteJailCell(next.getPrisonName(), next.getCellName(), null);
            }
        };
    }
}
