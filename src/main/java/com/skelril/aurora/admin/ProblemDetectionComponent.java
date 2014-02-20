/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.admin;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Server;

import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * User: Wyatt Childers
 * Date: 11/11/13
 */
@ComponentInformation(friendlyName = "Problem Detector", desc = "Problem detection system.")
public class ProblemDetectionComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private final DecimalFormat format = new DecimalFormat("#.#");

    private long lastTickWarning = 0, lastMemoryWarning = 0;
    private long lastTick = 0;
    private double averageError = 0;

    @Override
    public void enable() {

        //server.getScheduler().runTaskTimer(inst, this, 20, 20);
    }

    @Override
    public void run() {

        try {
            checkMemory();
        } catch (Exception ex) {
            if (lastMemoryWarning == 0 || System.currentTimeMillis() - lastMemoryWarning > 1000) {
                System.gc();
                server.broadcastMessage(ChatColor.RED + "[WARNING] " + ex.getMessage());
                lastMemoryWarning = System.currentTimeMillis();
            }
        }

        try {
            checkTicks();
        } catch (Exception ex) {
            if (lastTickWarning == 0 || System.currentTimeMillis() - lastTickWarning > 1000) {
                server.broadcastMessage(ChatColor.RED + "[WARNING] " + ex.getMessage());
                lastTickWarning = System.currentTimeMillis();
            }
        }
    }

    public void checkTicks() throws Exception {

        if (lastTick == 0) {
            lastTick = System.currentTimeMillis();
            return;
        }
        long elapsedTime = System.currentTimeMillis() - lastTick;

        double error = (1000 - elapsedTime) * -1;

        averageError = (error + averageError) / 2;
        lastTick = System.currentTimeMillis();

        if (averageError < 50) return;

        throw new Exception("Slow clock rate, Current error: " + format.format(error) +
                ", AVG error: " + format.format(averageError) + "!");
    }

    public void checkMemory() throws Exception {

        double memory = Math.floor(Runtime.getRuntime().freeMemory() / 1024.0 / 1024.0);

        if (memory < 70) {
            throw new Exception("Low RAM: " + memory + " MB!");
        }
    }
}
