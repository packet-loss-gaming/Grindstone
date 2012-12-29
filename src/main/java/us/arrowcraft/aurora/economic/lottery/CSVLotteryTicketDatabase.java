package us.arrowcraft.aurora.economic.lottery;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.player.GenericWealthStore;
import us.arrowcraft.aurora.util.player.WealthStore;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CSVLotteryTicketDatabase implements LotteryTicketDatabase {

    private final Logger log = CommandBook.inst().getLogger();

    protected final File lotteryTicketFile;
    protected final List<GenericWealthStore> tickets = new ArrayList<>();
    protected Map<String, GenericWealthStore> nameTicket = new HashMap<>();

    public CSVLotteryTicketDatabase(File ticketStorageDir) {

        lotteryTicketFile = new File(ticketStorageDir, "lotterytickets.csv");

    }

    @Override
    public synchronized boolean load() {

        FileInputStream input = null;
        boolean successful = true;

        try {
            input = new FileInputStream(lotteryTicketFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 2) {
                    log.warning("A lottery ticket entry with < 3 fields was found!");
                    continue;
                }
                try {
                    String ownerName = line[0].trim();
                    int count = Integer.parseInt(line[1].trim());
                    if ("".equals(ownerName) || "null".equals(ownerName)) ownerName = null;
                    GenericWealthStore ticket = new WealthStore(ownerName, count);
                    if (ownerName != null) nameTicket.put(ownerName.toLowerCase(), ticket);
                    tickets.add(ticket);
                } catch (NumberFormatException e) {
                    log.warning("The value or count of one or more tickets could not be read!");
                }
            }
            log.info(tickets.size() + " lottery ticket entries loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            log.warning("Failed to load " + lotteryTicketFile.getAbsolutePath() + ": " + e.getMessage());
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
            output = new FileOutputStream(lotteryTicketFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (GenericWealthStore lotteryTicket : tickets) {
                line = new String[] {
                        lotteryTicket.getOwnerName().trim(),
                        String.valueOf(lotteryTicket.getValue())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + lotteryTicketFile.getAbsolutePath()
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
    public boolean ticketExist(Player player) {

        return ticketExist(player.getName());
    }

    @Override
    public boolean ticketExist(String playerName) {

        GenericWealthStore lotteryTicket = getLotteryTicket(playerName.trim().toLowerCase());
        return lotteryTicket != null;
    }

    @Override
    public void addTicket(Player player, int count) {

        addTicket(player.getName(), count);
    }

    @Override
    public void addTicket(String playerName, int count) {

        if (ticketExist(playerName)) {
            getLotteryTicket(playerName).setValue(getLotteryTicket(playerName).getValue() + count);
        } else {
            GenericWealthStore lotteryTicket = new WealthStore(playerName.trim(), count);
            nameTicket.put(playerName.trim().toLowerCase(), lotteryTicket);
            tickets.add(lotteryTicket);
        }
    }

    @Override
    public void removeTicket(String playerName) {

        if (!ticketExist(playerName)) return;
        GenericWealthStore lotteryTicket = getLotteryTicket(playerName);
        tickets.remove(lotteryTicket);
        nameTicket.remove(playerName.trim().toLowerCase());
    }

    @Override
    public void clearTickets() {

        tickets.clear();
        nameTicket.clear();
    }

    @Override
    public List<GenericWealthStore> getTickets() {

        return tickets;
    }

    @Override
    public GenericWealthStore getLotteryTicket(Player player) {

        return getLotteryTicket(player.getName());
    }

    @Override
    public GenericWealthStore getLotteryTicket(String playerName) {

        return nameTicket.get(playerName.trim().toLowerCase());
    }

    @Override
    public Iterator<GenericWealthStore> iterator() {

        return new Iterator<GenericWealthStore>() {

            private final Iterator<GenericWealthStore> setIter = tickets.iterator();
            private GenericWealthStore next;

            public boolean hasNext() {

                return setIter.hasNext();
            }

            public GenericWealthStore next() {

                return next = setIter.next();
            }

            public void remove() {

                removeTicket(next.getOwnerName());
            }
        };
    }
}
