package us.arrowcraft.aurora.jail;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.ChatUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class CSVInmateDatabase implements InmateDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final Logger auditLogger
            = Logger.getLogger("Minecraft.CommandBook.Jail");
    protected final File inmateFile;

    /**
     * Used to lookup inmates by name
     */
    protected Map<String, Inmate> nameInmate = new HashMap<>();

    /**
     * A set of all inmates
     */
    protected final Set<Inmate> inmates = new HashSet<>();

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public CSVInmateDatabase(File inmateStorageDir) {

        inmateFile = new File(inmateStorageDir, "inmates.csv");

        // Set up an audit trail
        try {
            FileHandler handler = new FileHandler(
                    (new File(inmateStorageDir, "inmates.%g.%u.log")).getAbsolutePath()
                            .replace("\\", "/"), true);

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
                    String name = line[0].trim().toLowerCase();
                    String prisonName = line[1].trim().toLowerCase();
                    String reason = line[2].trim().toLowerCase();
                    long startDate = Long.parseLong(line[3]);
                    long endDate = Long.parseLong(line[4]);
                    if ("".equals(name) || "null".equals(name))
                        name = null;
                    Inmate inmate = new Inmate(name, prisonName, reason, startDate, endDate);
                    if (name != null)
                        nameInmate.put(name, inmate);
                    inmates.add(inmate);
                } catch (NumberFormatException e) {
                    log.warning("Non-long long field found in inmate!");
                }
            }
            log.info(inmates.size() + " jailed name(s) loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            nameInmate = new HashMap<>();
            log.warning("Failed to load " + inmateFile.getAbsolutePath()
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

    public synchronized boolean save() {

        FileOutputStream output = null;
        boolean successful = true;

        try {
            output = new FileOutputStream(inmateFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (Inmate inmate : inmates) {
                line = new String[] {
                        inmate.getName().trim().toLowerCase(),
                        inmate.getPrisonName().trim().toLowerCase(),
                        inmate.getReason().trim(),
                        String.valueOf(inmate.getStart()),
                        String.valueOf(inmate.getEnd())
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

    public boolean isJailedName(String name) {

        name = name.trim().toLowerCase();
        Inmate inmate = nameInmate.get(name);
        if (inmate != null) {
            if (inmate.getEnd() != 0L && inmate.getEnd() - System.currentTimeMillis() <= 0) {
                unjail(name, null, "Temp-jail expired");
                save();
                return false;
            }
            return true;
        }
        return false;
    }

    public void jail(Player player, String prisonName, CommandSender source, String reason, long end) {

        jail(player.getName(), prisonName, source, reason, end);
    }

    public void jail(String name, String prisonName, CommandSender source, String reason, long end) {

        Inmate inmate = new Inmate(name.trim().toLowerCase(), prisonName.trim().toLowerCase(),
                reason.trim(), System.currentTimeMillis(), end);
        String jailedName;
        if (name != null) {
            name = name.trim().toLowerCase();
            nameInmate.put(name, inmate);
            jailedName = name;
            inmates.add(inmate);
            auditLogger.info(String.format("JAIL: %s jailed %s: %s",
                    source == null ? "Plugin" : PlayerUtil.toUniqueName(source),
                    jailedName,
                    reason.trim()));
        }
    }

    public boolean unjail(Player player, CommandSender source, String reason) {

        return unjail(player.getName(), source, reason);
    }

    public boolean unjail(String name, CommandSender source, String reason) {

        Inmate inmate = null;
        String jailedName = null;
        if (name != null) {
            name = name.trim().toLowerCase();
            inmate = nameInmate.remove(name);
            if (inmate != null) {
                jailedName = name;
            }
        }
        if (inmate != null) {
            inmates.remove(inmate);
            auditLogger.info(String.format("UNJAIL: %s unjailed %s: %s",
                    source == null ? "Plugin" : PlayerUtil.toUniqueName(source),
                    jailedName,
                    reason.trim()));
            try {
                Player player = Bukkit.getPlayerExact(name);
                player.teleport(player.getWorld().getSpawnLocation());
                ChatUtil.sendNotice(player, "You have been unjailed.");
            } catch (Exception ignored) {
            }
            return true;
        }
        return false;
    }

    public String getJailedNameMessage(String name) {

        Inmate inmate = nameInmate.get(name.trim().toLowerCase());
        if (inmate == null || inmate.getReason() == null) return "You are jailed.";
        return inmate.getReason().trim();
    }

    public Iterator<Inmate> iterator() {

        return new Iterator<Inmate>() {

            private final Iterator<Inmate> setIter = inmates.iterator();
            private Inmate next;

            public boolean hasNext() {

                return setIter.hasNext();
            }

            public Inmate next() {

                return next = setIter.next();
            }

            public void remove() {

                unjail(next.getName(), null, "Removed by iterator");
            }
        };
    }

    public Inmate getJailedName(String name) {

        return nameInmate.get(name.trim().toLowerCase());
    }

    @Override
    public List<Inmate> getInmatesList() {

        return new ArrayList<Inmate>(inmates);
    }
}