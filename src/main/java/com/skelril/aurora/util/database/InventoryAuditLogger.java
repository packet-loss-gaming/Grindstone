/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.database;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Author: Turtle9598
 */
public class InventoryAuditLogger {

    private final Logger log = CommandBook.inst().getLogger();
    protected final Logger inventoryLogger = Logger.getLogger("Minecraft.CommandBook.Inventory");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public InventoryAuditLogger(File storageDir) {

        // Set up the purchase logger
        try {
            FileHandler loggingHandler = new FileHandler((new File(storageDir, "inventory.log")).getAbsolutePath().replace("\\", "/"), true);

            loggingHandler.setFormatter(new java.util.logging.Formatter() {

                @Override
                public String format(LogRecord record) {

                    return "[" + dateFormat.format(new Date()) + "] " + record.getMessage() + "\r\n";
                }
            });

            loggingHandler.setFilter(new Filter() {
                @Override
                public boolean isLoggable(LogRecord record) {

                    return record.getMessage().startsWith("INVENTORY DUMP - ");
                }
            });

            inventoryLogger.addHandler(loggingHandler);
        } catch (SecurityException | IOException e) {
            log.warning("Failed to setup the inventory audit log: " + e.getMessage());
        }
    }

    public void log(String playerName, ItemStack itemStack) {

        if (itemStack == null || itemStack.getTypeId() == BlockID.AIR) return;

        // Log the item stack
        String name = itemStack.getItemMeta().hasDisplayName() ? itemStack.getItemMeta().getDisplayName() : "";


        inventoryLogger.info("INVENTORY DUMP - " + playerName + " - " + itemStack.getAmount() + " - " + itemStack.getType().toString() + "("
                + itemStack.getDurability() + ")" + (!name.isEmpty() ? " - " + name : "") + ".");
    }

    public boolean unload() {

        for (Handler handler : inventoryLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                handler.flush();
                handler.close();
                inventoryLogger.removeHandler(handler);
                return true;
            }
        }
        return false;
    }
}
