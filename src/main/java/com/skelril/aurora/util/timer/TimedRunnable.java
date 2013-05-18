package com.skelril.aurora.util.timer;

import org.bukkit.scheduler.BukkitTask;

public class TimedRunnable implements Runnable {

    private BukkitTask task;
    private IntegratedRunnable action;

    private int times;

    public TimedRunnable(IntegratedRunnable action, int times) {

        this.action = action;
        this.times = times + 1;
    }

    public void setTask(BukkitTask task) {

        this.task = task;
    }

    @Override
    public void run() {

        times = times - 1;

        if (times > 0) {
            action.run(times);
        } else {
            cancel(true);
        }
    }

    public void cancel() {

        cancel(false);
    }

    public void cancel(boolean withEnd) {

        if (withEnd) action.end();
        task.cancel();
    }
}
