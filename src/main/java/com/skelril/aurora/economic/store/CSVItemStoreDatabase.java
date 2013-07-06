package com.skelril.aurora.economic.store;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CSVItemStoreDatabase implements ItemStoreDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final File itemFile;

    /**
     * Used to lookup cells by name
     */
    protected Map<String, ItemPricePair> nameItemPrice = new HashMap<>();

    public CSVItemStoreDatabase(File cellStorageDir) {

        itemFile = new File(cellStorageDir, "store.csv");
    }

    @Override
    public synchronized boolean load() {

        FileInputStream input = null;
        boolean successful = true;

        try {
            input = new FileInputStream(itemFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 2) {
                    log.warning("A cell entry with < 2 fields was found!");
                    continue;
                }
                try {
                    String name = line[0].trim().toLowerCase();
                    double price = Double.parseDouble(line[1]);
                    if (name.isEmpty() || "null".equals(name)) name = null;
                    ItemPricePair itemPair = new ItemPricePair(name, price);
                    if (name != null) {
                        nameItemPrice.put(name, itemPair);
                    }
                } catch (NumberFormatException e) {
                    log.warning("Non-int int field found in store record!");
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
    public void addItem(String name, double price) {

        if (name == null || name.isEmpty()) return;
        name = name.trim().toLowerCase();
        nameItemPrice.put(name, new ItemPricePair(name, price));
    }

    @Override
    public void removeItem(String name) {

        if (name == null || name.isEmpty()) return;
        nameItemPrice.remove(name.toLowerCase());
    }

    @Override
    public ItemPricePair getItem(String name) {

        return nameItemPrice.get(name.trim().toLowerCase());
    }

    @Override
    public List<ItemPricePair> getItemList() {

        return Lists.newArrayList(nameItemPrice.values());
    }
}
