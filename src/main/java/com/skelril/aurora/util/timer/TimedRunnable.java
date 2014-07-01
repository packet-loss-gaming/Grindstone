/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.timer;

import org.bukkit.scheduler.BukkitTask;

public class TimedRunnable implements Runnable {

    private BukkitTask task;
    private IntegratedRunnable action;

    private int times;
    private boolean done = false;

    public TimedRunnable(IntegratedRunnable action, int times) {
        this.action = action;
        this.times = times;
    }

    public boolean isComplete() {
        return done;
    }

    public void addTime(int times) {
        this.times += times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getTimes() {
        return times;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    @Override
    public void run() {
        if (times > 0) {
            if (action.run(times)) {
                times--;
            }
        } else {
            cancel(true);
        }
    }

    public void cancel() {
        cancel(false);
    }

    public void cancel(boolean withEnd) {

        if (done) return; // Task is done

        if (withEnd) action.end();
        task.cancel();
        done = true;
    }
}
