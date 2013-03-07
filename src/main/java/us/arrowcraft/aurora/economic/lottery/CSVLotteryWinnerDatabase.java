package us.arrowcraft.aurora.economic.lottery;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CSVLotteryWinnerDatabase implements LotteryWinnerDatabase {

    private final Logger log = CommandBook.inst().getLogger();

    protected final File lotteryWinnersFile;
    protected final List<Winner> winners = new ArrayList<>();

    public CSVLotteryWinnerDatabase(File ticketStorageDir) {

        lotteryWinnersFile = new File(ticketStorageDir, "lotterywinners.csv");
    }

    @Override
    public synchronized boolean load() {

        FileInputStream input = null;
        boolean successful = true;

        try {
            input = new FileInputStream(lotteryWinnersFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 2) {
                    log.warning("A lottery winner entry with < 2 fields was found!");
                    continue;
                }
                try {
                    String ownerName = line[0].trim();
                    long time = Long.parseLong(line[1].trim());
                    if (ownerName.isEmpty() || "null".equals(ownerName)) {
                        log.warning("Invalid lottery winner entry found!");
                        continue;
                    }
                    winners.add(new Winner(ownerName, time));
                } catch (NumberFormatException e) {
                    log.warning("The time could not be read!");
                }
            }
            log.info(winners.size() + " lottery winner entries loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            log.warning("Failed to load " + lotteryWinnersFile.getAbsolutePath() + ": " + e.getMessage());
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
            output = new FileOutputStream(lotteryWinnersFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            Collections.sort(winners);

            short max = 10;
            for (Winner winner : winners) {
                if (max > 0) max--;
                else break;

                line = new String[] {
                        winner.getName(),
                        String.valueOf(winner.getTime())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + lotteryWinnersFile.getAbsolutePath() + ": " + e.getMessage());
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
    public void addWinner(String name) {

        addWinner(name, System.currentTimeMillis());
    }

    @Override
    public void addWinner(String name, long time) {

        winners.add(new Winner(name, time));
    }

    @Override
    public List<String> getRecentWinner() {

        Collections.sort(winners);

        List<String> names = new ArrayList<>();
        Iterator<Winner> it = winners.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            names.add(it.next().getName());
        }
        return Collections.unmodifiableList(names);
    }

    @Override
    public List<String> getRecentWinner(int limit) {

        Collections.sort(winners);

        List<String> names = new ArrayList<>(limit);
        Iterator<Winner> it = winners.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext() && limit > 0) {
            limit--;
            names.add(it.next().getName());
        }
        return Collections.unmodifiableList(names);
    }

    @Override
    public Iterator<String> iterator() {

        return new Iterator<String>() {

            private final Iterator<Winner> setIter = winners.iterator();
            private String next;

            public boolean hasNext() {

                return setIter.hasNext();
            }

            public String next() {

                return next = setIter.next().getName();
            }

            @Override
            public void remove() {

                setIter.remove();
            }
        };
    }

    private class Winner implements Comparable {

        private final String name;
        private final Long winTime;

        public Winner(String name, Long winTime) {

            this.name = name;
            this.winTime = winTime;
        }

        public String getName() {

            return name;
        }

        public Long getTime() {

            return winTime;
        }

        @Override
        public int compareTo(Object o) {

            if (o == null || !(o instanceof Winner)) return -1;

            Winner winner = (Winner) o;
            if (this.getTime().equals(winner.getTime())) return 0;
            if (this.getTime() < winner.getTime()) return 1;
            return -1;
        }
    }
}