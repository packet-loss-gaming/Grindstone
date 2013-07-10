package com.skelril.aurora.economic.store;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.database.UnloadableDatabase;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * Author: Turtle9598
 */
public class CSVItemStoreDatabase implements ItemStoreDatabase, UnloadableDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final File itemFile;
    protected final Logger storeLogger = Logger.getLogger("Minecraft.CommandBook.Store");

    /**
     * Used to lookup cells by name
     */
    protected Map<String, ItemPricePair> nameItemPrice = new HashMap<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public CSVItemStoreDatabase(File storageDir) {

        itemFile = new File(storageDir, "store.csv");

        // Set up the purchase logger
        try {
            FileHandler purchaseHandler = new FileHandler((new File(storageDir, "purchases.log")).getAbsolutePath().replace("\\", "/"), true);

            purchaseHandler.setFormatter(new java.util.logging.Formatter() {

                @Override
                public String format(LogRecord record) {

                    return "[" + dateFormat.format(new Date()) + "] " + record.getMessage() + "\r\n";
                }
            });

            purchaseHandler.setFilter(new Filter() {
                @Override
                public boolean isLoggable(LogRecord record) {

                    return record.getMessage().contains("purchased") || record.getMessage().contains("sold");
                }
            });

            storeLogger.addHandler(purchaseHandler);

            FileHandler changeHandler = new FileHandler((new File(storageDir, "store-db.log")).getAbsolutePath().replace("\\", "/"), true);

            changeHandler.setFormatter(new java.util.logging.Formatter() {

                @Override
                public String format(LogRecord record) {

                    return "[" + dateFormat.format(new Date()) + "] " + record.getMessage() + "\r\n";
                }
            });

            changeHandler.setFilter(new Filter() {
                @Override
                public boolean isLoggable(LogRecord record) {

                    return record.getMessage().contains("set") || record.getMessage().contains("removed");
                }
            });

            storeLogger.addHandler(changeHandler);
        } catch (SecurityException | IOException e) {
            log.warning("Failed to setup audit log for the CSV cell database: " + e.getMessage());
        }
    }

    @Override
    public synchronized boolean load() {

        nameItemPrice.clear();

        FileInputStream input = null;
        boolean successful = true;

        try {
            input = new FileInputStream(itemFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                try {
                    String name = "null";
                    double price = 0;
                    boolean disableBuy = false;
                    boolean disableSell = false;

                    for (int i = 0; i < line.length; i++) {
                        switch (i) {
                            case 0:
                                name = line[i].trim().toLowerCase();
                                break;
                            case 1:
                                price = Double.parseDouble(line[i]);
                                break;
                            case 2:
                                disableBuy = Boolean.parseBoolean(line[i]);
                                break;
                            case 3:
                                disableSell = Boolean.parseBoolean(line[i]);
                                break;
                        }
                    }

                    if (name.isEmpty() || "null".equals(name)) name = null;
                    ItemPricePair itemPair = new ItemPricePair(name, price, disableBuy, disableSell);
                    if (name != null) {
                        nameItemPrice.put(name, itemPair);
                    }
                } catch (NumberFormatException e) {
                    log.warning("Non-numerical field found in numerical store record!");
                }
            }
            log.info(nameItemPrice.size() + " item(s) loaded for the admin store.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            nameItemPrice = new HashMap<>();
            log.warning("Failed to load " + itemFile.getAbsolutePath() + ": " + e.getMessage());
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
            output = new FileOutputStream(itemFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            List<ItemPricePair> items = getItemList();
            Collections.sort(items);
            for (ItemPricePair itemPair : items) {
                line = new String[]{
                        itemPair.getName().trim().toLowerCase(),
                        String.valueOf(itemPair.getPrice()),
                        String.valueOf(!itemPair.isBuyable()),
                        String.valueOf(!itemPair.isSellable())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + itemFile.getAbsolutePath() + ": " + e.getMessage());
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
    public void addItem(String playerName, String itemName, double price, boolean disableBuy, boolean disableSell) {

        if (itemName == null || itemName.isEmpty()) return;
        itemName = itemName.trim().toLowerCase();
        nameItemPrice.put(itemName, new ItemPricePair(itemName, price, disableBuy, disableSell));

        // Log the change
        String buyingState = "purchase is " + (disableBuy ? "enabled" : "disabled");
        String sellState = "sale is " + (disableSell ? "enabled" : "disabled");
        storeLogger.info(playerName + " set the price of: " + itemName.toUpperCase() + " to " + price + ", " + buyingState + " and " + sellState + ".");
    }

    @Override
    public void removeItem(String playerName, String itemName) {

        if (itemName == null || itemName.isEmpty()) return;
        nameItemPrice.remove(itemName.toLowerCase());

        // Log the change
        storeLogger.info(playerName + " removed: " + itemName.toUpperCase() + " from the database.");
    }

    @Override
    public void logTransaction(String playerName, String itemName, int amount) {

        int absoluteAmount = Math.abs(amount);
        // Log the change
        storeLogger.info(playerName + (amount < absoluteAmount ? " sold: " : " purchased: ") + absoluteAmount + " " + itemName.toUpperCase() + ".");
    }

    @Override
    public ItemPricePair getItem(String name) {

        return nameItemPrice.get(name.trim().toLowerCase());
    }

    @Override
    public List<ItemPricePair> getItemList() {

        return Lists.newArrayList(nameItemPrice.values());
    }

    @Override
    public boolean unload() {

        for (Handler handler : storeLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                handler.flush();
                handler.close();
                storeLogger.removeHandler(handler);
                return true;
            }
        }
        return false;
    }
}
