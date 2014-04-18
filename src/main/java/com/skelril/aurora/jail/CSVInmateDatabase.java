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
import com.sk89q.commandbook.util.entity.player.UUIDUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import static com.sk89q.commandbook.CommandBook.logger;

public class CSVInmateDatabase implements InmateDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final Logger auditLogger
            = Logger.getLogger("Minecraft.CommandBook.Jail");
    protected final File inmateFile;

    /**
     * Used to lookup inmates by name
     */
    protected Map<UUID, Inmate> UUIDInmate = new HashMap<>();

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public CSVInmateDatabase(File inmateStorageDir) {

        inmateFile = new File(inmateStorageDir, "inmates.csv");

        // Set up an audit trail
        try {
            FileHandler handler = new FileHandler(
                    (new File(inmateStorageDir, "inmates.%g.%u.log")).getAbsolutePath()
                            .replace("\\", "/"), true
            );

            handler.setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {

                    return "[" + dateFormat.format(new Date())
                            + "] " + record.getMessage() + "\r\n";
                }
            });

            auditLogger.addHandler(handler);
        } catch (SecurityException | IOException e) {
            log.warning("Failed to setup audit log for the "
                    + "CSV inmate database: " + e.getMessage());
        }
    }

    public synchronized boolean load() {

        FileInputStream input = null;
        boolean successful = true;
        boolean needsSaved = false;

        try {
            input = new FileInputStream(inmateFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 5) {
                    log.warning("A jail entry with < 5 fields was found!");
                    continue;
                }
                try {
                    String rawID = "null";
                    String prisonName = "lava-flow";
                    String reason = "";
                    long startDate = 0;
                    long endDate = 0;
                    boolean isMuted = false;

                    for (int i = 0; i < line.length; i++) {
                        switch (i) {
                            case 0:
                                rawID = line[i].trim().toLowerCase();
                                break;
                            case 1:
                                prisonName = line[i].trim().toLowerCase();
                                break;
                            case 2:
                                reason = line[i].trim().toLowerCase();
                                break;
                            case 3:
                                startDate = Long.parseLong(line[i]);
                                break;
                            case 4:
                                endDate = Long.parseLong(line[i]);
                                break;
                            case 5:
                                isMuted = Boolean.parseBoolean(line[i]);
                                break;
                        }
                    }
                    if ("".equals(rawID) || "null".equals(rawID)) rawID = null;
                    Inmate inmate = new Inmate(prisonName, reason, startDate, endDate, isMuted);
                    try {
                        inmate.setID(UUID.fromString(rawID));
                    } catch (IllegalArgumentException ex) {
                        logger().finest("Converting inmate " + rawID + "'s name to UUID...");
                        UUID creatorID = UUIDUtil.convert(rawID);
                        if (creatorID != null) {
                            inmate.setID(creatorID);
                            needsSaved = true;
                            logger().finest("Success!");
                        } else {
                            inmate.setName(rawID);
                            logger().warning("Inmate " + rawID + "'s name could not be converted!");
                        }
                    }
                    UUIDInmate.put(inmate.getID(), inmate);
                } catch (NumberFormatException e) {
                    log.warning("Non-long long field found in inmate!");
                }
            }
            log.info(UUIDInmate.size() + " jailed name(s) loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            UUIDInmate = new HashMap<>();
            log.warning("Failed to load " + inmateFile.getAbsolutePath() + ": " + e.getMessage());
            successful = false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (needsSaved) save();
        return successful;
    }

    public synchronized boolean save() {

        FileOutputStream output = null;
        boolean successful = true;

        try {
            output = new FileOutputStream(inmateFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (Inmate inmate : UUIDInmate.values()) {
                line = new String[]{
                        String.valueOf(inmate.getID()),
                        inmate.getPrisonName().trim().toLowerCase(),
                        inmate.getReason() == null ? "" : inmate.getReason(),
                        String.valueOf(inmate.getStart()),
                        String.valueOf(inmate.getEnd()),
                        String.valueOf(inmate.isMuted())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + inmateFile.getAbsolutePath()
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
    public boolean isInmate(UUID ID) {
        return UUIDInmate.containsKey(ID);
    }

    @Override
    public Inmate getInmate(UUID ID) {
        return UUIDInmate.get(ID);
    }

    @Override
    public void jail(Player player, String prisonName, CommandSender source, String reason, long end, boolean isMuted) {
        jail(player.getUniqueId(), prisonName, source, reason, end, isMuted);
    }

    @Override
    public void jail(UUID ID, String prison, CommandSender source, String reason, long end, boolean mute) {
        Validate.notNull(ID);
        Validate.notNull(prison);
        Validate.notNull(reason);

        prison = prison.trim().toLowerCase();
        reason = reason.trim();

        long start = System.currentTimeMillis();

        if (isInmate(ID)) {
            Inmate inmate = UUIDInmate.remove(ID);
            start = inmate.getStart();
        }

        Inmate inmate = new Inmate(ID, prison, reason, start, end, mute);
        UUIDInmate.put(ID, inmate);
        auditLogger.info(String.format("JAIL: %s jailed %s: %s", source == null ? "Plugin" : ChatUtil.toUniqueName(source), ID, reason.trim()));
    }

    @Override
    public boolean unjail(Player player, CommandSender source, String reason) {
        return unjail(player.getUniqueId(), source, reason);
    }

    @Override
    public boolean unjail(UUID ID, CommandSender source, String reason) {

        Validate.notNull(ID);

        Inmate inmate = UUIDInmate.remove(ID);
        if (inmate != null) {
            auditLogger.info(String.format("UNJAIL: %s unjailed %s: %s",
                    source == null ? "Plugin" : ChatUtil.toUniqueName(source),
                    inmate.getID(),
                    reason.trim()));
            return true;
        }
        return false;
    }

    public Iterator<Inmate> iterator() {

        return new Iterator<Inmate>() {

            private final Iterator<Map.Entry<UUID, Inmate>> setIter = UUIDInmate.entrySet().iterator();
            private Inmate next;

            public boolean hasNext() {
                return setIter.hasNext();
            }

            public Inmate next() {
                next = setIter.next().getValue();
                return next;
            }

            public void remove() {
                unjail(next.getID(), null, "Removed by iterator");
            }
        };
    }

    @Override
    public List<Inmate> getInmatesList() {
        return Lists.newArrayList(UUIDInmate.values());
    }
}